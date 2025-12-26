/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthapp.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.concurrent.futures.await
import android.os.Vibrator
import android.util.Log
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.ComparisonType
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeCondition
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseTypeCapabilities
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.data.WarmUpConfig
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.prepareExercise
import androidx.health.services.client.resumeExercise
import androidx.health.services.client.startExercise
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.example.healthapp.service.ExerciseLogger
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import androidx.core.net.toUri
import com.example.healthapp.service.DynamicTimeWarping
import com.example.healthapp.service.RealityCheck
import com.google.android.gms.tasks.Tasks
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Entry point for [HealthServicesClient] APIs, wrapping them in coroutine-friendly APIs.
 */
@SuppressLint("RestrictedApi")
@Singleton
class ExerciseClientManager
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    healthServicesClient: HealthServicesClient,
    private val logger: ExerciseLogger,
    private val vibrator: Vibrator,
    private val dataStoreManager: DataStoreManager,
    private val realityCheck: RealityCheck,
    private val sensorManager: SensorManager
) {
    val exerciseClient: ExerciseClient = healthServicesClient.exerciseClient
    var breathingExerciseJob: Job? = null
    private val managerScope = CoroutineScope(Dispatchers.Default)
    private val heartRateCriticalMutableFlow = MutableStateFlow(false)
    val heartRateCritical = heartRateCriticalMutableFlow.asStateFlow()
    private val tenMinutesPassedMutableFlow = MutableStateFlow(false)
    var tenMinutesPassed = tenMinutesPassedMutableFlow.asStateFlow()
    private var tenMinutesTimerJob: Job? = null
    private var i = 0
    private val monitoringEndedMutableFlow = MutableStateFlow(false)
    val monitoringEnded = monitoringEndedMutableFlow.asStateFlow()
    val prePanicTemplate = DtwTemplates.Companion.prePanicSmoothedTemplate()
    val postPanicTemplate = DtwTemplates.Companion.postPanicSmoothedTemplate()
    val restingHrTemplate = DtwTemplates.Companion.restingHrTemplateSmoothedS11()
    val panicTemplate = DtwTemplates.Companion.panicSmoothedTemplate()
    private val dtwWindow = mutableListOf<Double>()
    private val dtwPrePanicSampleCount = 300
    private var heartRateSensor: Sensor? = null
    private val powerManager =
        applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wakeLock: PowerManager.WakeLock =
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HealthApp::MonitoringWakeLock")

    suspend fun getExerciseCapabilities(): ExerciseTypeCapabilities? {
        val capabilities = exerciseClient.getCapabilities()

        return if (ExerciseType.RUNNING in capabilities.supportedExerciseTypes) {
            capabilities.getExerciseTypeCapabilities(ExerciseType.RUNNING)
        } else {
            null
        }
    }

    /***
     * Note: don't call this method from outside of ExerciseService.kt
     * when acquiring calories or distance.
     */
    suspend fun prepareExercise() {
        logger.log("Preparing an exercise")
        val warmUpConfig =
            WarmUpConfig(
                exerciseType = ExerciseType.RUNNING,
                dataTypes = setOf(DataType.HEART_RATE_BPM, DataType.LOCATION)
            )
        try {
            exerciseClient.prepareExercise(warmUpConfig)
        } catch (e: Exception) {
            logger.log("Prepare exercise failed - ${e.message}")
        }
    }

    suspend fun endMonitoring() {
        logger.log("Ending monitoring")
        stopHrMonitoring()
        if (exerciseClient.isExerciseInProgress()) {
            exerciseClient.endExercise()
        }
    }

    suspend fun endBreathingExercise() {
        logger.log("Ending exercise")
        stopHrMonitoring()
        if (exerciseClient.isExerciseInProgress()) {
            exerciseClient.endExercise()
        }
    }

    suspend fun pauseExercise() {
        logger.log("Pausing exercise")
        exerciseClient.pauseExercise()
    }

    suspend fun resumeExercise() {
        logger.log("Resuming exercise")
        exerciseClient.resumeExercise()
    }

    /**
     * When the flow starts, it will register an [ExerciseUpdateCallback] and start to emit
     * messages. When there are no more subscribers, or when the coroutine scope is
     * cancelled, this flow will unregister the listener.
     * [callbackFlow] is used to bridge between a callback-based API and Kotlin flows.
     */
    val exerciseUpdateFlow =
        callbackFlow {
            val callback =
                object : ExerciseUpdateCallback {
                    override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                        logMetrics(update.latestMetrics)
                        //evaluateData(update.latestMetrics)
                        trySendBlocking(ExerciseMessage.ExerciseUpdateMessage(update))
                    }

                    override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
                        trySendBlocking(ExerciseMessage.LapSummaryMessage(lapSummary))
                    }

                    override fun onRegistered() {
                    }

                    override fun onRegistrationFailed(throwable: Throwable) {
                        TODO("Not yet implemented")
                    }

                    override fun onAvailabilityChanged(
                        dataType: DataType<*, *>,
                        availability: Availability
                    ) {
                        if (availability is LocationAvailability) {
                            trySendBlocking(
                                ExerciseMessage.LocationAvailabilityMessage(availability)
                            )
                        }
                    }
                }

            exerciseClient.setUpdateCallback(callback)
            awaitClose {
                // Ignore async result
                exerciseClient.clearUpdateCallbackAsync(callback)
            }
        }

    private companion object {
        const val CALORIES_THRESHOLD = 250.0
    }


    fun startBreathingExercise() {
        tenMinutesPassedMutableFlow.value = false
        tenMinutesTimerJob?.cancel()
        if (breathingExerciseJob?.isActive != true) {
            Log.i("ExerciseClientManager", "Starting breathing exercise")
            //val startTime = System.currentTimeMillis()
            breathingExerciseJob = realityCheck.executeVibration(managerScope, vibrator)
        }
        tenMinutesTimerJob = managerScope.launch {
            delay(60000 * 10) // wait 10 min and ask again
            // signal panicViewModel that we need to show dialog...
            Log.i("ExerciseClientManager", "10 minutes have passed...")
            tenMinutesPassedMutableFlow.value = true
        }
    }

    fun stopBreathingExercise() {
        tenMinutesPassedMutableFlow.value = false
        tenMinutesTimerJob?.cancel()
        if (breathingExerciseJob?.isActive == true) {
            Log.i("ExerciseClientManager", "Stopping breathing exercise")
            realityCheck.stopVibrating(vibrator)
            breathingExerciseJob?.cancel()
            breathingExerciseJob = null
        }
    }

    fun resetCriticalHeartRateState() {
        heartRateCriticalMutableFlow.value = false
    }

    fun setCriticalHeartRateToPreviousState() {
        heartRateCriticalMutableFlow.value = !heartRateCriticalMutableFlow.value
    }

    var tempHR = 0.0
    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            Log.i("ExerciseClientManager", "Sensor accuracy changed: $p1")
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
                val heartRate = event.values[0]

                if (heartRate > 0) { // Ignoriere ungültige Werte

                    Log.i(
                        "ExerciseClientManager",
                        "Heart rate via Sensor: $heartRate, Time: ${event.timestamp}"
                    )
                    if (i >= 600) {
                        i = 0
                    }
                    if (i < 300) {
                        tempHR = prePanicTemplate[i]

                    } else {
                        tempHR = postPanicTemplate[i - 300]
                    }
                    i++
                    // 1. Neuen Punkt zum DTW-Fenster hinzufügen
                    while (dtwWindow.size >= dtwPrePanicSampleCount) {
                        dtwWindow.removeAt(0)
                    }
                    dtwWindow.add(tempHR)//heartRate.toDouble())

                    Log.i("ExerciseClientManager", "HR Windowsize: ${dtwWindow.size}")

                    // 3. DTW-Analyse starten
                    if (dtwWindow.size > 280) { // TODO auf 250 oder so machen
                        Log.i(
                            "ExerciseClientManager",
                            "Got at least 1 minute of Data. Size: ${dtwWindow.size}. Starting DTW analysis"
                        )
                        runDtwAnalysis()
                    }
                }
            }
        }

        var calmDtwDistance: Double = Double.MAX_VALUE
        var dtwDistance: Double = Double.MAX_VALUE


        private fun runDtwAnalysis() {
            managerScope.launch {
                // Extrahiere nur die HR-Werte aus deinem Fenster
                val liveHrValues = dtwWindow.toDoubleArray()
                // WICHTIG: Interpoliere die Live-Daten, damit sie die gleiche Länge wie das Template haben!
                // DTW kann mit unterschiedlich langen Reihen umgehen, aber für den Vergleich ist gleiche Länge besser.
                val interpolatedLiveValues = interpolate(liveHrValues, prePanicTemplate.size)

                calmDtwDistance = min(
                    DynamicTimeWarping.calculateDistance(
                        postPanicTemplate,
                        interpolatedLiveValues
                    ),
                    DynamicTimeWarping.calculateDistance(restingHrTemplate, interpolatedLiveValues)
                )
                dtwDistance = min(
                    DynamicTimeWarping.calculateDistance(
                        prePanicTemplate,
                        interpolatedLiveValues
                    ), DynamicTimeWarping.calculateDistance(panicTemplate, interpolatedLiveValues)
                )

                //val dtwThreshold = 100000.0 // Hoher Threshold, da in diesem Usecase mehr false positives als false negatives sinnvoll wären

                Log.w(
                    "DTW_Analysis",
                    "PA PATTERNS panic Distance: ${dtwDistance}, calm Distance: $calmDtwDistance"
                )

                if (dtwDistance <= calmDtwDistance) {
                    heartRateCriticalMutableFlow.value = true
                } else {
                    // wenn nach 10sek trotz calm hr die Übung ausgeführt wird, wird dieses False egal sein solange der Wert nicht zwischendurch auf true gewechselt ist
                    // das ist gut, weil: wenn man nicht "calm" war, so wird eine weitere Calm-erkennung mit denselben Mustern nicht einen direkten Unterscheid aufweisen
                    // bzw... dann würde sowieso die ganze Zeit der Dialog gespammt werden mit denselben Ergebnissen.
                    heartRateCriticalMutableFlow.value = false
                }
            }
        }

        // Nötig für 780 bis 900 Einträge also zu Beginn des Monitorings (auch bei Neustart)
        private fun interpolate(source: DoubleArray, newSize: Int): DoubleArray {
            if (source.size == newSize) return source
            val result = DoubleArray(newSize)
            val factor = (source.size - 1).toDouble() / (newSize - 1)
            for (i in 0 until newSize) {
                val srcIndex = i * factor
                val i0 = srcIndex.toInt()
                val i1 = min(i0 + 1, source.size - 1)
                if (i0 == i1) {
                    result[i] = source[i0]
                } else {
                    val w1 = srcIndex - i0
                    val w0 = 1.0 - w1
                    result[i] = w0 * source[i0] + w1 * source[i1]
                }
            }
            return result
        }
    }

    suspend fun startHrMonitoring() {
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire()
        monitoringEndedMutableFlow.value = false
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heartRateSensor == null) {
            val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
            Log.e("ExerciseClientManager", "No heartrate sensor detected. Sensors: $sensorList")
            return
        }

        sensorManager.registerListener(sensorListener, heartRateSensor, 1_000_000)

        // hier wird die Companionapp gestartet
        val exec: Executor = Executors.newSingleThreadExecutor()
        val connectedNodes = Tasks.await(Wearable.getNodeClient(applicationContext).connectedNodes)
        Log.i(
            "ExerciseClientManager",
            "Connected nodes: $connectedNodes. Trying to start remote companion."
        )
        if (!connectedNodes.isEmpty()) {
            val remoteActivityHelper = RemoteActivityHelper(applicationContext, exec)
            remoteActivityHelper.startRemoteActivity(
                Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData("companionapp://sms92".toUri()),
                connectedNodes[0].id
            ).await()

            try {
                Log.i("WearOSRemote", "Remote Activity Started?")
            } catch (e: Exception) {
                Log.e("WearOSRemote", "Fehler beim Starten der Remote Activity", e)
            }
        } else {
            Log.i(
                "ExerciseClientService",
                "No connected devices detected. Cannot launch remote Companion."
            )
        }
    }

    private fun stopHrMonitoring() {
        sensorManager.unregisterListener(sensorListener)
        tenMinutesTimerJob?.cancel()
        tenMinutesTimerJob = null
        monitoringEndedMutableFlow.value = true
        heartRateCriticalMutableFlow.value = false
        tenMinutesPassedMutableFlow.value = false
        stopBreathingExercise()
        heartRateSensor = null
        dtwWindow.clear()
        if (wakeLock.isHeld) {
            wakeLock.release()
            Log.i("ExerciseClientManager", "WakeLock released.")
        }
        Log.i("DTW_Sensor", "SensorListener deregistriert.")
    }
}


private fun logMetrics(metrics: DataPointContainer) {
    metrics.getData(DataType.HEART_RATE_BPM).forEach { dataPoint ->
        val bpm = dataPoint.value
        val timeStamp = dataPoint.timeDurationFromBoot // Oder eine andere Zeitangabe
        Log.d(
            "ExerciseService_HR_Background",
            "Measured Heartrate (im Service): $bpm BPM, Zeitstempel: $timeStamp"
        )

    }
}


data class Thresholds(
    var distance: Double,
    var duration: Duration,
    var durationIsSet: Boolean = duration != Duration.ZERO,
    var distanceIsSet: Boolean = distance != 0.0
)

sealed class ExerciseMessage {
    class ExerciseUpdateMessage(
        val exerciseUpdate: ExerciseUpdate
    ) : ExerciseMessage()

    class LapSummaryMessage(
        val lapSummary: ExerciseLapSummary
    ) : ExerciseMessage()

    class LocationAvailabilityMessage(
        val locationAvailability: LocationAvailability
    ) : ExerciseMessage()
}
