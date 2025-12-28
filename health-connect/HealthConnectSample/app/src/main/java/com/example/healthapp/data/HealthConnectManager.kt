/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthapp.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources.NotFoundException
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.example.healthapp.R
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.io.InvalidObjectException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KClass

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

@Singleton
/** Demonstrates reading and writing from Health Connect. */
class HealthConnectManager(private val context: Context, private val dataStoreManager: DataStoreManager): DataClient.OnDataChangedListener {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    val PHONE_A_FRIEND = "/phone_a_friend"
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var smsManager: SmsManager = SmsManager.getDefault()

    private val _smsPermissionGranted = MutableStateFlow(hasSmsPermission())
    // 2. Exponiere es als öffentliches, nur lesbares StateFlow
    val smsPermissionGranted: StateFlow<Boolean> = _smsPermissionGranted.asStateFlow()

    // 3. Eine öffentliche Funktion, um den Status zu prüfen und das Flow zu aktualisieren
    fun updateSmsPermissionStatus() {
        _smsPermissionGranted.value = hasSmsPermission()
    }

    // 4. Eine private Hilfsfunktion, die den tatsächlichen Systemstatus prüft
    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    private val changesDataTypes = setOf(
        ExerciseSessionRecord::class,
        StepsRecord::class,
        SpeedRecord::class,
        DistanceRecord::class,
        TotalCaloriesBurnedRecord::class,
        HeartRateRecord::class,
        SleepSessionRecord::class
    )

    var permissionsGranted = mutableStateOf(false)
        private set

    val permissions = changesDataTypes.map { HealthPermission.getReadPermission(it) }.toSet()

    init {
        // 3. Registriere den Listener, wenn der Manager erstellt wird
        dataClient.addListener(this)
        Log.d("HealthConnectManager", "DataChangedListener registriert.")
    }
    val healthConnectCompatibleApps by lazy {
        val intent = Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE")

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )
        }

        packages.associate {
            val icon = try {
                context.packageManager.getApplicationIcon(it.activityInfo.packageName)
            } catch (e: NotFoundException) {
                null
            }
            val label = context.packageManager.getApplicationLabel(it.activityInfo.applicationInfo)
                .toString()
            it.activityInfo.packageName to
                    HealthConnectAppInfo(
                        packageName = it.activityInfo.packageName,
                        icon = icon,
                        appLabel = label
                    )
        }
    }

    var availability = mutableStateOf(SDK_UNAVAILABLE)
        private set

    fun checkAvailability() {
        availability.value = HealthConnectClient.getSdkStatus(context)
    }

    init {
        checkAvailability()
    }

    /**
     * Determines whether all the specified permissions are already granted. It is recommended to
     * call [PermissionController.getGrantedPermissions] first in the permissions flow, as if the
     * permissions are already granted then there is no need to request permissions via
     * [PermissionController.createRequestPermissionResultContract].
     */
    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun revokeAllPermissions() {
        healthConnectClient.permissionController.revokeAllPermissions()
    }

    /**
     * Obtains a list of [ExerciseSessionRecord]s in a specified time frame. An Exercise Session Record is a
     * period of time given to an activity, that would make sense to a user, e.g. "Afternoon run"
     * etc. It does not necessarily mean, however, that the user was *running* for that entire time,
     * more that conceptually, this was the activity being undertaken.
     */
    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSession> {
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)

        Log.i("HealthConnectManager", "readExerciseSessions: ${response.records}")

        val returnVal = response.records.map { record ->
            val packageName = record.metadata.dataOrigin.packageName
            ExerciseSession(
                startTime = dateTimeWithOffsetOrDefault(record.startTime, record.startZoneOffset),
                endTime = dateTimeWithOffsetOrDefault(record.startTime, record.startZoneOffset),
                id = record.metadata.id,
                sourceAppInfo = healthConnectCompatibleApps[packageName],
                title = record.title
            )
        }

        return returnVal
    }

    suspend fun readInitUserInputState(): Boolean {
        return dataStoreManager.readInitUserInputState().first()
    }

    suspend fun saveInitUserInputState(state: Boolean) {
        dataStoreManager.saveInitUserInputState(state)
    }

    suspend fun saveExerciseChoice(choice: String) {
        dataStoreManager.saveExerciseChoice(choice)
        sendExerciseChoiceToWatch(choice)
    }

    /**
     * Writes an [ExerciseSessionRecord] to Health Connect, and additionally writes underlying data for
     * the session too, such as [StepsRecord], [DistanceRecord] etc.
     */
    suspend fun writeExerciseSession(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): InsertRecordsResponse {
        return healthConnectClient.insertRecords(
            listOf(
                ExerciseSessionRecord(
                    metadata = Metadata.manualEntry(),
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = "My Run #${Random.nextInt(0, 60)}"
                ),
                StepsRecord(
                    metadata = Metadata.manualEntry(),
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    count = (1000 + 1000 * Random.nextInt(3)).toLong()
                ),
                DistanceRecord(
                    metadata = Metadata.manualEntry(),
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    distance = Length.meters((1000 + 100 * Random.nextInt(20)).toDouble())
                ),
                TotalCaloriesBurnedRecord(
                    metadata = Metadata.manualEntry(),
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    energy = Energy.calories(140 + (Random.nextInt(20)) * 0.01)
                )
            ) + buildHeartRateSeries(start, end)
        )
    }

    /**
     * Deletes an [ExerciseSessionRecord] and underlying data.
     */
    suspend fun deleteExerciseSession(uid: String) {
        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        healthConnectClient.deleteRecords(
            ExerciseSessionRecord::class,
            recordIdsList = listOf(uid),
            clientRecordIdsList = emptyList()
        )
        val timeRangeFilter = TimeRangeFilter.between(
            exerciseSession.record.startTime,
            exerciseSession.record.endTime
        )
        val rawDataTypes: Set<KClass<out Record>> = setOf(
            HeartRateRecord::class,
            SpeedRecord::class,
            DistanceRecord::class,
            StepsRecord::class,
            TotalCaloriesBurnedRecord::class
        )
        rawDataTypes.forEach { rawType ->
            healthConnectClient.deleteRecords(rawType, timeRangeFilter)
        }
    }

    /**
     * Reads aggregated data and raw data for selected data types, for a given [ExerciseSessionRecord].
     */
    suspend fun readAssociatedSessionData(
        uid: String
    ): ExerciseSessionData {
        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        // Use the start time and end time from the session, for reading raw and aggregate data.
        val timeRangeFilter = TimeRangeFilter.between(
            startTime = exerciseSession.record.startTime,
            endTime = exerciseSession.record.endTime
        )
        val aggregateDataTypes = setOf(
            ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
            StepsRecord.COUNT_TOTAL,
            DistanceRecord.DISTANCE_TOTAL,
            TotalCaloriesBurnedRecord.ENERGY_TOTAL,
            HeartRateRecord.BPM_AVG,
            HeartRateRecord.BPM_MAX,
            HeartRateRecord.BPM_MIN,
        )
        // Limit the data read to just the application that wrote the session. This may or may not
        // be desirable depending on the use case: In some cases, it may be useful to combine with
        // data written by other apps.
        val dataOriginFilter = setOf(exerciseSession.record.metadata.dataOrigin)
        val aggregateRequest = AggregateRequest(
            metrics = aggregateDataTypes,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = dataOriginFilter)
        val aggregateData = healthConnectClient.aggregate(aggregateRequest)

        return ExerciseSessionData(
            uid = uid,
            totalActiveTime = aggregateData[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL],
            totalSteps = aggregateData[StepsRecord.COUNT_TOTAL],
            totalDistance = aggregateData[DistanceRecord.DISTANCE_TOTAL],
            totalEnergyBurned = aggregateData[TotalCaloriesBurnedRecord.ENERGY_TOTAL],
            minHeartRate = aggregateData[HeartRateRecord.BPM_MIN],
            maxHeartRate = aggregateData[HeartRateRecord.BPM_MAX],
            avgHeartRate = aggregateData[HeartRateRecord.BPM_AVG],
        )
    }

    /**
     * Deletes all existing sleep data.
     */
    suspend fun deleteAllSleepData() {
        val now = Instant.now()
        healthConnectClient.deleteRecords(SleepSessionRecord::class, TimeRangeFilter.before(now))
    }

    /**
     * Generates a week's worth of sleep data using a [SleepSessionRecord] to describe the overall
     * period of sleep, with multiple [SleepSessionRecord.Stage] periods which cover the entire
     * [SleepSessionRecord]. For the purposes of this sample, the sleep stage data is generated randomly.
     */
    suspend fun generateSleepData() {
        val records = mutableListOf<Record>()
        // Make yesterday the last day of the sleep data
        val lastDay = ZonedDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS)
        val notes = context.resources.getStringArray(R.array.sleep_notes_array)
        // Create 7 days-worth of sleep data
        for (i in 0..7) {
            val wakeUp = lastDay.minusDays(i.toLong())
                .withHour(Random.nextInt(7, 10))
                .withMinute(Random.nextInt(0, 60))
            val bedtime = wakeUp.minusDays(1)
                .withHour(Random.nextInt(19, 22))
                .withMinute(Random.nextInt(0, 60))
            val sleepSession = SleepSessionRecord(
                metadata = Metadata.manualEntry(),
                notes = notes[Random.nextInt(0, notes.size)],
                startTime = bedtime.toInstant(),
                startZoneOffset = bedtime.offset,
                endTime = wakeUp.toInstant(),
                endZoneOffset = wakeUp.offset,
                stages = generateSleepStages(bedtime, wakeUp)
            )
            records.add(sleepSession)
        }
        healthConnectClient.insertRecords(records)
    }

    suspend fun generateHrvData() {
        val records = mutableListOf<Record>()
        // Create 7 days-worth of sleep data
        for (i in 0..7) {
            val measurementTime = ZonedDateTime.now().minusDays(i+1L).truncatedTo(ChronoUnit.DAYS)
            val hrvRecord = HeartRateVariabilityRmssdRecord(
                metadata = Metadata.manualEntry(),
                time = measurementTime.toInstant(),
                zoneOffset = measurementTime.offset,
                heartRateVariabilityMillis = (20..100).shuffled().first().toDouble()
            )
            records.add(hrvRecord)
        }
        healthConnectClient.insertRecords(records)
    }

    suspend fun deleteAllHrvData() {
        val now = Instant.now()
        healthConnectClient.deleteRecords(HeartRateVariabilityRmssdRecord::class, TimeRangeFilter.before(now))
    }

    suspend fun generateRestingHrData() {
        val records = mutableListOf<Record>()
        // Create 7 days worth of restingHR data
        for (i in 0..7){
            val measurementTime = ZonedDateTime.now().minusDays(i+1L).truncatedTo(ChronoUnit.DAYS)
            val rhrRecord = RestingHeartRateRecord(
                metadata = Metadata.manualEntry(),
                time = measurementTime.toInstant(),
                zoneOffset = measurementTime.offset,
                beatsPerMinute = (60..85).shuffled().first().toLong()
            )
            records.add(rhrRecord)
        }
        healthConnectClient.insertRecords(records)
        Log.i("HealthConnectManager", "Generated resting HR data: $records")
    }

    suspend fun deleteAllRestingHrData() {
        val now = Instant.now()
        healthConnectClient.deleteRecords(RestingHeartRateRecord::class, TimeRangeFilter.before(now))
    }

    suspend fun deleteAllExerciseData() {
        val now = Instant.now()
        healthConnectClient.deleteRecords(ExerciseSessionRecord::class, TimeRangeFilter.before(now))
    }
    /**
     * Reads sleep sessions for the previous seven days (from yesterday) to show a week's worth of
     * sleep data.
     *
     * In addition to reading [SleepSessionRecord]s, for each session, the duration is calculated to
     * demonstrate aggregation, and the underlying [SleepSessionRecord.Stage] data is also read.
     */
    suspend fun readSleepSessions(): List<SleepSessionData> {
        val lastDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            .minusDays(1)
            .withHour(12)
        val firstDay = lastDay
            .minusDays(7)

        val sessions = mutableListOf<SleepSessionData>()
        val sleepSessionRequest = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )
        val sleepSessions = healthConnectClient.readRecords(sleepSessionRequest)
        sleepSessions.records.forEach { session ->
            val sessionTimeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            val durationAggregateRequest = AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = sessionTimeFilter
            )
            val aggregateResponse = healthConnectClient.aggregate(durationAggregateRequest)
            sessions.add(
                SleepSessionData(
                    uid = session.metadata.id,
                    title = session.title,
                    notes = session.notes,
                    startTime = session.startTime,
                    startZoneOffset = session.startZoneOffset,
                    endTime = session.endTime,
                    endZoneOffset = session.endZoneOffset,
                    duration = aggregateResponse[SleepSessionRecord.SLEEP_DURATION_TOTAL],
                    stages = session.stages
                )
            )
        }
        return sessions
    }

    /**
     * Returns the weekly average of [WeightRecord]s.
     */
//    suspend fun computeWeeklyAverage(start: Instant, end: Instant): Mass? {
//        val request = AggregateRequest(
//            metrics = setOf(WeightRecord.WEIGHT_AVG),
//            timeRangeFilter = TimeRangeFilter.between(start, end)
//        )
//        val response = healthConnectClient.aggregate(request)
//        return response[WeightRecord.WEIGHT_AVG]
//    }


    /**
     * Obtains a changes token for the specified record types.
     */
    suspend fun getChangesToken(dataTypes: Set<KClass<out Record>>): String {
        val request = ChangesTokenRequest(dataTypes)
        return healthConnectClient.getChangesToken(request)
    }

    /**
     * Creates a [Flow] of change messages, using a changes token as a start point. The flow will
     * terminate when no more changes are available, and the final message will contain the next
     * changes token to use.
     */
     fun getChanges(token: String): Flow<ChangesMessage> = flow {
        var nextChangesToken = token
        do {
            val response = healthConnectClient.getChanges(nextChangesToken)
            if (response.changesTokenExpired) {
                // As described here: https://developer.android.com/guide/health-and-fitness/health-connect/data-and-data-types/differential-changes-api
                // tokens are only valid for 30 days. It is important to check whether the token has
                // expired. As well as ensuring there is a fallback to using the token (for example
                // importing data since a certain date), more importantly, the app should ensure
                // that the changes API is used sufficiently regularly that tokens do not expire.
                throw IOException("Changes token has expired")
            }
            emit(ChangesMessage.ChangeList(response.changes))
            nextChangesToken = response.nextChangesToken
        } while (response.hasMore)
        emit(ChangesMessage.NoMoreChanges(nextChangesToken))
    }

    /** Creates a random sleep stage that spans the specified [start] to [end] time. */
    private fun generateSleepStages(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<SleepSessionRecord.Stage> {
        val sleepStages = mutableListOf<SleepSessionRecord.Stage>()
        var stageStart = start
        while (stageStart < end) {
            val stageEnd = stageStart.plusMinutes(Random.nextLong(30, 120))
            val checkedEnd = if (stageEnd > end) end else stageEnd
            sleepStages.add(
                SleepSessionRecord.Stage(
                    stage = randomSleepStage(),
                    startTime = stageStart.toInstant(),
                    endTime = checkedEnd.toInstant()))
            stageStart = checkedEnd
        }
        return sleepStages
    }

    /**
     * Convenience function to fetch a time-based record and return series data based on the record.
     * Record types compatible with this function must be declared in the
     * [com.example.healthapp.presentation.screen.recordlist.RecordType] enum.
     */
    suspend fun fetchSeriesRecordsFromUid(recordType: KClass<out Record>, uid: String, seriesRecordsType: KClass<out Record>): List<Record> {
        val recordResponse = healthConnectClient.readRecord(recordType, uid)
        // Use the start time and end time from the session, for reading raw and aggregate data.
        val timeRangeFilter =
            when (recordResponse.record) {
                // Change to use series record instead
                is ExerciseSessionRecord -> {
                    val record = recordResponse.record as ExerciseSessionRecord
                    TimeRangeFilter.between(startTime = record.startTime, endTime = record.endTime)
                }
                is SleepSessionRecord -> {
                    val record = recordResponse.record as SleepSessionRecord
                    TimeRangeFilter.between(startTime = record.startTime, endTime = record.endTime)
                }
                else -> {
                    throw InvalidObjectException("Record with unregistered data type returned")
                }
            }

        // Limit the data read to just the application that wrote the session. This may or may not
        // be desirable depending on the use case: In some cases, it may be useful to combine with
        // data written by other apps.
        val dataOriginFilter = setOf(recordResponse.record.metadata.dataOrigin)
        val request =
            ReadRecordsRequest(
                recordType = seriesRecordsType,
                dataOriginFilter = dataOriginFilter,
                timeRangeFilter = timeRangeFilter)
        return healthConnectClient.readRecords(request).records
    }

    private fun buildHeartRateSeries(
        sessionStartTime: ZonedDateTime,
        sessionEndTime: ZonedDateTime
    ): HeartRateRecord {
        val samples = mutableListOf<HeartRateRecord.Sample>()
        var time = sessionStartTime
        while (time.isBefore(sessionEndTime)) {
            samples.add(
                HeartRateRecord.Sample(
                    time = time.toInstant(), beatsPerMinute = (80 + Random.nextInt(80)).toLong()))
            time = time.plusSeconds(30)
        }
        return HeartRateRecord(
            metadata = Metadata.manualEntry(),
            startTime = sessionStartTime.toInstant(),
            startZoneOffset = sessionStartTime.offset,
            endTime = sessionEndTime.toInstant(),
            endZoneOffset = sessionEndTime.offset,
            samples = samples)
    }

    fun isFeatureAvailable(feature: Int): Boolean{
        return healthConnectClient
            .features
            .getFeatureStatus(feature) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }

    // Represents the two types of messages that can be sent in a Changes flow.
    sealed class ChangesMessage {
        data class NoMoreChanges(val nextChangesToken: String) : ChangesMessage()

        data class ChangeList(val changes: List<Change>) : ChangesMessage()
    }

    // Hier wird der Stressindex anhand von Messdaten und ggf. Nutzerangeben berechnet.
    // Je niedriger er ist, desto besser
    suspend fun calculateStress(): Int {
        Log.d("calculateStress", "calculateStress called")

        val end = ZonedDateTime.now().toInstant()
        val start = ZonedDateTime.now().minusDays(7).toInstant()
        // fetch data for calc
        val exerciseSessionRecord = readExerciseSessions(start, end)
        val sleep = readSleepSessions()
        val anxietyIndex = dataStoreManager.readAnxietyScore().first()
        val eventConfirmation = if(dataStoreManager.readEventConfirmation().first()) 0 else 100
        // calculate indices for more complex data and meanings
        val sleepIndex = calculateSleepIndex(sleep) // Je höher desto besser
        val hrvIndex = calculateHrvIndex() // Je höher desto besser
        Log.i("calculateStress", "sleepIndex: $sleepIndex, hrvIndex: $hrvIndex, anxietyIndex: $anxietyIndex, eventConfirmation: $eventConfirmation")
        val exerciseIndex = calculateExerciseIndex(exerciseSessionRecord) // Je höher desto besser
        val stressIndex = 100 - ((sleepIndex + hrvIndex + exerciseIndex + anxietyIndex + eventConfirmation) / 5)
        if(stressIndex != dataStoreManager.readStressIndex().first()) {
            scope.launch {
                val contactnumber = dataStoreManager.readEmergencyNumber().first()
                Log.i("MessageListener", "Contactnumber: $contactnumber")
                if(!contactnumber.isEmpty() && contactnumber != "#") {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED

                    if(hasPermission) {
                        val msg = "Sie erhalten diese Nachricht, da Nutzer xy den Mental Health Assistant verwendet. Es liegt ein Stressindex von $stressIndex vor."
                        smsManager.sendTextMessage(contactnumber, null, msg, null, null)
                    }
                }
            }
            dataStoreManager.saveStressIndex(stressIndex)
            sendMessageToWatch(stressIndex)
        }
        return stressIndex
    }

    /*
    Hier wird der Schlafindex berechnet. Er ist ein Kennwert für die Schlafqualität, welche sich wiederum auf
    die Psyche und den Stress der Nutzer auswirkt. Die Herangehensweise ist aus https://kiwi-health.de/en/sleep-quality-and-sleep-index/
    entnommen.
     */
    fun calculateSleepIndex(sleep: List<SleepSessionData>): Int {
        if(sleep.isEmpty()) {
            return 0
        }
        val totalSleepDuration = sleep[0].duration?.toMillis()?.div(1000.0)
        val optimalDepth = totalSleepDuration?.times(0.45) ?: 0.0

        val duration = (sleep[0].duration?.toMillis()?.div(1000.0)?.div(8*60*60)?.times(100))?.roundToInt()
            ?:0
        var deepSleep = 0.0
        var remSleep = 0.0
        var interruptions = 0.0

        // hier werden die Schlafphasen-Dauern einzeln erhoben
        sleep[0].stages.forEach{ stage ->
            val stageDuration = stage.endTime.epochSecond - stage.startTime.epochSecond
            when(stage.stage) {
                SleepSessionRecord.STAGE_TYPE_DEEP -> deepSleep += stageDuration
                SleepSessionRecord.STAGE_TYPE_REM -> remSleep += stageDuration
                SleepSessionRecord.STAGE_TYPE_AWAKE -> interruptions += 1
            }
        }

        val depth = (((deepSleep+remSleep)/optimalDepth)*100).roundToInt().coerceIn(0, 100)
        val regularity = 100 - (calculateSleepRegularity(sleep)/(2*60*60)).roundToInt() // Timecap bei 2 Stunden angelegt.
        var interruptionsIndex = ((interruptions/7)*100).roundToInt()
        interruptionsIndex = 100 - interruptionsIndex


        var timeUntilAsleep = 0L
        // Nimmt alle Phasen, die nicht als eine Art Schlaf wahrgenommen wurden VOR dem ersten Schlafeintrag
        // und addiert die Dauer dieser Phasen zusammen für die Einschlafdauer
        sleep[0].stages.takeWhile { stage ->
            stage.stage != SleepSessionRecord.STAGE_TYPE_REM &&
                    stage.stage != SleepSessionRecord.STAGE_TYPE_DEEP &&
                    stage.stage != SleepSessionRecord.STAGE_TYPE_LIGHT &&
                    stage.stage != SleepSessionRecord.STAGE_TYPE_SLEEPING
        }.forEach { s -> timeUntilAsleep += s.endTime.epochSecond - s.startTime.epochSecond }
        timeUntilAsleep = 100 - ((timeUntilAsleep/(30*60))*100).coerceIn(0, 100)

        val index = (duration.times(0.4) + depth*0.25 + regularity*0.2 + interruptionsIndex*0.1 + timeUntilAsleep*0.05)/100

        val resultSleepIndex = (index*100).roundToInt()
        Log.d("calculateSleepIndex", "totalSleepDuration: $totalSleepDuration, optimalDepth: $optimalDepth, duration: $duration, depth: $depth, regularity: $regularity, interruptionsIndex: $interruptionsIndex, timeUntilAsleep: $timeUntilAsleep")
        Log.d("calculateSleepIndex", "resultSleepIndex: $resultSleepIndex")

        return resultSleepIndex
    }

    private fun calculateSleepRegularity(sleep: List<SleepSessionData>): Double {
        // TODO hier noch Daten der letzten 14 Tage berücksichtigen? Aktuell umfassen die Einträge 7 Tage
        val startTimeInSeconds = sleep.map { data -> data.startTime.epochSecond }
        val endTimeInSeconds = sleep.map { data -> data.endTime.epochSecond }
        val observations = sleep.size

        val meanStart = startTimeInSeconds.average()
        var variance = startTimeInSeconds.map{
            (it-meanStart).pow(2)}.average()
        val stdStartTime = Math.sqrt(variance)

        val meanEnd = endTimeInSeconds.average()
        variance = endTimeInSeconds.map {
            (it-meanEnd).pow(2)}.average()
        val stdEndTime = Math.sqrt(variance)

        val meanCombined = (meanStart*observations + meanEnd*observations)/(observations*2)
        val dStart = meanStart - meanCombined
        val dEnd = meanEnd - meanCombined
        val stdCombined = Math.sqrt((observations*stdStartTime.pow(2)+observations*stdEndTime.pow(2)+observations*dStart.pow(2)+observations*dEnd.pow(2))/(observations*2))
        Log.d("calculateSleepRegularity", "stdStartTime: $stdStartTime, stdEndTime: $stdEndTime, stdCombined: $stdCombined")

        return stdCombined
    }

    // Berechne den aktuellen HRV-Index für die Stressberechnung und nutze ihn dann für den Durchschnitt der
    // Berechnung am Folgetag --> der Datensatz für das Einpendeln in einen "Normalwert" wird größer, bis er 2 Monate abdeckt
    // Je niedriger die HRV umso eher sei man wohl gestresst
    private suspend fun calculateHrvIndex(): Int {
        val currentHrv = dataStoreManager.readCurrentHrv().first()
        var hrvIndex = currentHrv / dataStoreManager.readHrvAvg().first()
        if(currentHrv > 70) {
            hrvIndex = 100
        } else {
            hrvIndex = (hrvIndex * 50).coerceIn(0,100)
        }

        val hrvDataList = dataStoreManager.readHrvData().first().split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
        hrvDataList.add(dataStoreManager.readCurrentHrv().first())
        if(hrvDataList.size > 60) {
            hrvDataList.removeAt(0)
        }
        dataStoreManager.saveHrvData(hrvDataList.average().roundToInt(), hrvDataList.toString())

        return hrvIndex
    }

    private fun calculateExerciseIndex(exerciseSession: List<ExerciseSession>) : Int {
        val weeklyDuration = exerciseSession.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }

        val exerciseIndex = (weeklyDuration / 3*60)*100 // Laut https://adaa.org/living-with-anxiety/managing-anxiety/exercise-stress-and-anxiety ist eine über die Woche verteilte Fitnessroutine besser als einmal 3 Stunden.

        return exerciseIndex.coerceIn(0,100).toInt()
    }

    suspend fun sendMessageToWatch() {
        try {
            val stressIndex = dataStoreManager.readStressIndex().first()
            val request = PutDataMapRequest.create("/measured_data").apply {
                dataMap.putInt("measured_data", stressIndex)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d("sendMessageToWatch", "Data sent: ${stressIndex}. DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d("sendMessageToWatch", "Saving DataItem failed: $exception")
        }
        try {
            val exerciseChoice = dataStoreManager.readExerciseChoice().first()
            val request = PutDataMapRequest.create("/exercise_choice").apply {
                dataMap.putString("choice", exerciseChoice)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d("sendMessageToWatch", "Data sent: ${exerciseChoice}. DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d("sendMessageToWatch", "Saving DataItem failed: $exception")
        }
    }

    private suspend fun sendMessageToWatch(stressIndex: Int) {

//        val end = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1)
//        val start = end
//            .minusDays(7) // resting HR wird nur als Anhaltspunkt für die Thresholds verwendet. Vielleicht weiterer Nutzen für direkte Vergleichswerte mit zB Vortag?
//
//        val request = ReadRecordsRequest(
//            recordType = RestingHeartRateRecord::class,
//            timeRangeFilter = TimeRangeFilter.between(start.toInstant(), end.toInstant())
//        )

        //val restingHrData = healthConnectClient.readRecords(request).records.map { it.beatsPerMinute }

        //Log.d("sendMessageToWatch", "restingHR: $restingHrData")
        //val listToSend = arrayOf(30, stressIndex).toMutableList()

        try {
            val request = PutDataMapRequest.create("/measured_data").apply {
                dataMap.putInt("measured_data", stressIndex)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d("sendMessageToWatch", "Data sent: ${stressIndex}. DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d("sendMessageToWatch", "Saving DataItem failed: $exception")
        }
    }

    private suspend fun sendExerciseChoiceToWatch(choice: String) {

        try {
            val request = PutDataMapRequest.create("/exercise_choice").apply {
                dataMap.putString("choice", choice)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d("sendMessageToWatch", "Data sent: ${choice}. DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d("sendMessageToWatch", "Saving DataItem failed: $exception")
        }
    }
    //effort to try and send stressmeasurement results to wear os app

    suspend fun insertExerciseSession() {
        val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val latestStartOfSession = ZonedDateTime.now().minusMinutes(30)
        val offset = Random.nextDouble()

        // Generate random start time between the start of the day and (now - 30mins).
        val startOfSession = startOfDay.plusSeconds(
            (Duration.between(startOfDay, latestStartOfSession).seconds * offset).toLong()
        )
        val endOfSession = startOfSession.plusMinutes(30)

        writeExerciseSession(startOfSession, endOfSession)
        val end = ZonedDateTime.now().toInstant()
        val start = ZonedDateTime.now().minusDays(1).toInstant()

        readExerciseSessions(start, end)
    }

    suspend fun generateAllData(){
        generateHrvData()
        //generateSleepData()
        insertExerciseSession()
        generateRestingHrData()
    }

    suspend fun deleteAllData() {
        Log.d("HealthConnectManager", "deleteAllData called")
        deleteAllHrvData()
        deleteAllSleepData() //klappt zumindest visuell nicht
        deleteAllRestingHrData()
        deleteAllExerciseData()
    }

    suspend fun saveEmergencyNumber(number: String) {
        dataStoreManager.saveEmergencyNumber(number)
    }

    suspend fun saveAppointmentInfo(choice: Boolean) {
        dataStoreManager.saveEventConfirmation(choice)
    }

    fun readEmergencyNumber(): Flow<String> {
        return dataStoreManager.readEmergencyNumber()
    }

    suspend fun saveFormular(number: Int) {
        dataStoreManager.saveAnxietyScore(number)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {

        dataEvents.forEach { dataEvent ->
            val uri = dataEvent.dataItem.uri
            when (uri.path) {
                PHONE_A_FRIEND -> {
                    val dataMapItem = DataMapItem.fromDataItem(dataEvent.dataItem)
                    val panicDetected = dataMapItem.dataMap.getBoolean("panic_detected")
                    Log.d("MessageListener", "Data from Watch received. Panic: $panicDetected")

                    scope.launch {
                        val contactnumber = dataStoreManager.readEmergencyNumber().first()
                        Log.i("MessageListener", "Contactnumber: $contactnumber")
                        if(!contactnumber.isEmpty() && contactnumber != "#") {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.SEND_SMS
                            ) == PackageManager.PERMISSION_GRANTED

                            if(hasPermission) {
                                val msg = "Sie erhalten diese Nachricht, da Nutzer xy den Mental Health Assistant verwendet. Es wurde eine Panikübung ausgelöst."
                                smsManager.sendTextMessage(contactnumber, null, msg, null, null)
                            }
                        }
                    }

                }
            }
        }
    }
}
