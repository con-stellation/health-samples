package com.example.healthapp.presentation.screen.userinputs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthConnectManager
import com.example.healthapp.presentation.screen.sleepsession.SleepSessionViewModel
import com.example.healthapp.presentation.screen.sleepsession.SleepSessionViewModel.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppointmentInfoViewModel (private val healthConnectManager: HealthConnectManager): ViewModel() {

    private val saveStateMutableFlow = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = saveStateMutableFlow.asStateFlow()
    fun saveAppointmentInfo(choice: Boolean) {
        viewModelScope.launch {
            try {
                healthConnectManager.saveAppointmentInfo(choice)
                healthConnectManager.calculateStress()
                saveStateMutableFlow.value = SaveState.Success
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

class AppointmentInfoViewModelFactory (
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppointmentInfoViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}