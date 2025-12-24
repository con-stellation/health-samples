package com.example.healthapp.presentation.screen.userinputs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthConnectManager
import kotlinx.coroutines.launch

class ExerciseChoiceViewModel(private val healthConnectManager: HealthConnectManager): ViewModel() {
    fun saveExerciseChoice(choice: String) {
        viewModelScope.launch {
            healthConnectManager.saveExerciseChoice(choice)
        }
    }
}

class ExerciseChoiceViewModelFactory (private val healthConnectManager: HealthConnectManager): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseChoiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExerciseChoiceViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

