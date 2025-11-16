package com.example.healthconnectsample.presentation.screen

import android.os.RemoteException
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthconnectsample.data.HealthConnectManager
import com.example.healthconnectsample.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.example.healthconnectsample.presentation.screen.exercisesession.ExerciseSessionViewModel.UiState
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class WelcomeScreenViewModel (val healthConnectManager: HealthConnectManager) :
    ViewModel() {

    val permissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class)
    )

    var permissionsGranted = mutableStateOf(false)
        private set

    var backgroundReadAvailable = mutableStateOf(false)
        private set

    var backgroundReadGranted = mutableStateOf(false)
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    fun initialLoad() {
        Log.i("WelcomeScreenViewModel", "Called initialload. Now doing read functions.")
        viewModelScope.launch {
            val permissionsGranted = healthConnectManager.hasAllPermissions(permissions)
            if (!permissionsGranted) {
                // Wenn die Berechtigungen fehlen, starte den Launcher.
                // Der WelcomeScreen wird diesen Launcher beobachten und starten.
                Log.d("WelcomeViewModel", "Permissions not granted. Preparing to launch permission request.")
                // Wir müssen dem UI nichts weiter mitteilen, da die Activity das Ergebnis
                // des Launchers abfängt und die Navigation entsprechend steuert.
            } else {
                Log.d("WelcomeViewModel", "All permissions are already granted.")
            }
        }
    }

}

class WelcomeScreenViewModelFactory(
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WelcomeScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WelcomeScreenViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}