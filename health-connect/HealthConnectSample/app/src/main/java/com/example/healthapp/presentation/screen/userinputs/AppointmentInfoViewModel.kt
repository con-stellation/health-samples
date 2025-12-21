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
import kotlinx.coroutines.launch

class AppointmentInfoViewModel (private val healthConnectManager: HealthConnectManager): ViewModel() {

    fun saveAppointmentInfo(choice: Boolean) {
        viewModelScope.launch {
            healthConnectManager.saveAppointmentInfo(choice)
            healthConnectManager.calculateStress()
        }
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