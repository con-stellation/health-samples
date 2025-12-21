package com.example.healthapp.data

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import com.example.healthapp.presentation.screen.exercisesession.ExerciseSessionViewModel
import java.time.ZonedDateTime

class HealthDataCenter (private val healthConnectManager: HealthConnectManager){

    // Reads all necessary healthdata for stresscalculation
    suspend fun fetchHealthData(exerciseSessionViewModel: ExerciseSessionViewModel?) {
        exerciseSessionViewModel?.initialLoad()
        Log.d("HealthDataCenter", "doing read for ExerciseSessions now")

        healthConnectManager.calculateStress()
    }
}