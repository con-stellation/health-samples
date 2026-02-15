package com.example.healthapp.presentation.screen.userinputs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseChoiceViewModel(private val healthConnectManager: HealthConnectManager): ViewModel() {
    private val saveStateMutableFlow = MutableStateFlow<SaveState>(
        SaveState.Idle)
    val saveState = saveStateMutableFlow.asStateFlow()
    fun saveExerciseChoice(choice: String) {
        viewModelScope.launch {
            try {
                healthConnectManager.saveExerciseChoice(choice)
                saveStateMutableFlow.value = SaveState.Success
            } catch (e: Exception) {
                saveStateMutableFlow.value = SaveState.Error(e)
            }
        }
    }

    fun readExerciseChoice(): Flow<String> {
        var choice: Flow<String> = MutableStateFlow("")
        try {
            choice = healthConnectManager.readExerciseChoice()
        } catch (e: Exception) {
            saveStateMutableFlow.value = SaveState.Error(e)
        }
        return choice
    }

    fun resetSaveState() {
        saveStateMutableFlow.value = SaveState.Idle
    }

    sealed class SaveState {
        object Idle : SaveState()
        object Success : SaveState()
        data class Error(val exception: Throwable) : SaveState()
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

