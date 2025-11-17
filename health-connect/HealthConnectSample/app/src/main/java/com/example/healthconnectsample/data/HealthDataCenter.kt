package com.example.healthconnectsample.data

import com.example.healthconnectsample.presentation.screen.exercisesession.ExerciseSessionViewModel

class HealthDataCenter (private val healthConnectManager: HealthConnectManager){

    suspend fun fetchHealthData(exerciseSessionViewModel: ExerciseSessionViewModel?) {
        exerciseSessionViewModel?.initialLoad()
        healthConnectManager.readHRV()
        healthConnectManager.calculateStress()
    }
}