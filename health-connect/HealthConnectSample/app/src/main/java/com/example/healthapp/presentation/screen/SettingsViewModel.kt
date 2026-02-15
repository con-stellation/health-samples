package com.example.healthapp.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val healthConnectManager: HealthConnectManager): ViewModel() {

    fun saveTapPreferences(choice: Boolean) {
        viewModelScope.launch {
            try {
                healthConnectManager.saveTapPreferences(choice)
            } catch (e: Exception) {
            }
        }
    }

    fun readTapPreferences(): Flow<Boolean> {
        var choice: Flow<Boolean> = MutableStateFlow(false)
        try {
            choice = healthConnectManager.readTapPreferences()
        } catch (e: Exception) {
        }
        return choice
    }

    fun readTeambuildingPreferences(): Flow<Boolean> {
        var choice: Flow<Boolean> = MutableStateFlow(false)
        try {
            choice = healthConnectManager.readTeambuildingPreferences()
        } catch (e: Exception) {
        }
        return choice
    }

    fun saveTeambuildingPreferences(choice: Boolean) {
        viewModelScope.launch {
            try {
                healthConnectManager.saveTeambuildingPreferences(choice)
            } catch (e: Exception) {
            }
        }
    }

    fun readCloudPreferences(): Flow<Boolean> {
        var choice: Flow<Boolean> = MutableStateFlow(false)
        try {
            choice = healthConnectManager.readCloudPreferences()
        } catch (e: Exception) {
        }
        return choice
    }

    fun saveCloudPreferences(choice: Boolean) {
        viewModelScope.launch {
            try {
                healthConnectManager.saveCloudPreferences(choice)
            } catch (e: Exception) {
            }
        }
    }

}

class SettingsViewModelFactory (private val healthConnectManager: HealthConnectManager): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

