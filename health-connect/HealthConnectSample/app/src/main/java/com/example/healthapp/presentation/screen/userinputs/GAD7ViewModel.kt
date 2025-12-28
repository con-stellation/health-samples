package com.example.healthapp.presentation.screen.userinputs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GAD7ViewModel (private val healthConnectManager: HealthConnectManager): ViewModel(){
    private val saveStateMutableFlow = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = saveStateMutableFlow.asStateFlow()
    fun saveGAD7Info(score: Int) {
        viewModelScope.launch {
            try {
                healthConnectManager.saveFormular(score)
                saveStateMutableFlow.value = SaveState.Success
                healthConnectManager.calculateStress()
            } catch (e: Exception) {
                saveStateMutableFlow.value = SaveState.Error(e)
            }
        }
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

class GAD7ViewModelFactory (private val healthConnectManager: HealthConnectManager): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GAD7ViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GAD7ViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}