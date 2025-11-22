package com.example.healthapp.data

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import com.example.healthapp.presentation.screen.exercisesession.ExerciseSessionViewModel
import java.time.ZonedDateTime

class HealthDataCenter (private val healthConnectManager: HealthConnectManager){

    var exerciseSessionsList: MutableState<List<ExerciseSession>> = mutableStateOf(listOf())
        private set
    var hrvSessionsList: MutableState<List<HeartRateVariabilityRmssdRecord>> = mutableStateOf(listOf())
        private set
    var sleepSessionList: MutableState<List<SleepSessionData>> = mutableStateOf(listOf())
        private set
    private val healthConnectCompatibleApps = healthConnectManager.healthConnectCompatibleApps

    // Reads all necessary healthdata for stresscalculation
    suspend fun fetchHealthData(exerciseSessionViewModel: ExerciseSessionViewModel?) {
        val end = ZonedDateTime.now().toInstant()
        val start = ZonedDateTime.now().minusDays(1).toInstant()
        exerciseSessionViewModel?.initialLoad()
        Log.d("HealthDataCenter", "doing read for ExerciseSessions now")
        exerciseSessionsList.value = healthConnectManager
            .readExerciseSessions(start, end)
            .map { record ->
                val packageName = record.metadata.dataOrigin.packageName
                ExerciseSession(
                    startTime = dateTimeWithOffsetOrDefault(record.startTime, record.startZoneOffset),
                    endTime = dateTimeWithOffsetOrDefault(record.startTime, record.startZoneOffset),
                    id = record.metadata.id,
                    sourceAppInfo = healthConnectCompatibleApps[packageName],
                    title = record.title
                )
            }

        hrvSessionsList.value = healthConnectManager.readHRV()
        sleepSessionList.value = healthConnectManager.readSleepSessions()
        healthConnectManager.calculateStress(hrvSessionsList.value, sleepSessionList.value)
    }
}