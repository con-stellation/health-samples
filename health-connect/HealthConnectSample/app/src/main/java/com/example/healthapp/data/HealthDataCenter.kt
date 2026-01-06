package com.example.healthapp.data

import android.util.Log
import com.example.healthapp.presentation.screen.exercisesession.ExerciseSessionViewModel

class HealthDataCenter (private val healthConnectManager: HealthConnectManager){

    // Reads all necessary healthdata for stresscalculation
    suspend fun fetchHealthData(exerciseSessionViewModel: ExerciseSessionViewModel?) {
        exerciseSessionViewModel?.initialLoad()
        Log.d("HealthDataCenter", "doing read for ExerciseSessions now")
        healthConnectManager.calculateStress()
        healthConnectManager.sendMessageToWatch() // sends stressIndex and exerciseChoicce to watch so it has all needed Data from the start

    }
}