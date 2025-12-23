package com.example.healthapp.presentation.screen.userinputs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhoneAFriendViewModel (private val healthConnectManager: HealthConnectManager): ViewModel() {

    private val saveStateMutableFlow = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = saveStateMutableFlow.asStateFlow()
    val smsPermissionsGranted = healthConnectManager.smsPermissionGranted

    val existingNumber: StateFlow<String> = healthConnectManager.readEmergencyNumber().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    fun savePhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            try {
                healthConnectManager.saveEmergencyNumber(phoneNumber)
                // 3. Setze den Zustand auf Erfolg
                saveStateMutableFlow.value = SaveState.Success
            } catch (e: Exception) {
                // Optional: Fehlerbehandlung
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

class PhoneAFriendViewModelFactory ( private val healthConnectManager: HealthConnectManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhoneAFriendViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhoneAFriendViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}