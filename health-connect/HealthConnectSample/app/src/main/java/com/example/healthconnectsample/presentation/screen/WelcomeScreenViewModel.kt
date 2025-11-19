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
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthconnectsample.data.HealthConnectManager
import com.example.healthconnectsample.data.HealthDataCenter
import com.example.healthconnectsample.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.example.healthconnectsample.presentation.screen.exercisesession.ExerciseSessionViewModel.UiState
import com.example.healthconnectsample.presentation.screen.recordlist.RecordListScreenViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import kotlin.collections.setOf

class WelcomeScreenViewModel (
    val healthConnectManager: HealthConnectManager,
    val exerciseSessionVM: ExerciseSessionViewModel,
    val healthDataCenter: HealthDataCenter
) :
    ViewModel() {

    val permissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
    )

    var permissionsGranted = mutableStateOf(false)
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    fun initialLoad() {
        Log.i("WelcomeScreenViewModel", "Called initialload. Now doing read functions.")
        viewModelScope.launch {
            val hasPermissions = healthConnectManager.hasAllPermissions(permissions)
            permissionsGranted.value = hasPermissions
            if (!hasPermissions) {
                // Wenn die Berechtigungen fehlen, starte den Launcher.
                // Der WelcomeScreen wird diesen Launcher beobachten und starten.
                Log.d("WelcomeViewModel", "Permissions not granted. Preparing to launch permission request.")
                // Wir müssen dem UI nichts weiter mitteilen, da die Activity das Ergebnis
                // des Launchers abfängt und die Navigation entsprechend steuert.
            } else {

                Log.d("WelcomeViewModel", "All permissions are already granted.")
                healthDataCenter.fetchHealthData(exerciseSessionVM)
            }
        }
    }

    fun generateAllData() {
        viewModelScope.launch {
            viewModelScope.launch {
                tryWithPermissionsCheck {
                    healthConnectManager.generateAllData()
                }
            }
        }
    }

    fun deleteAllData() {
        Log.d("WelcomeViewModel", "Deleting all data.")
        viewModelScope.launch {
            tryWithPermissionsCheck {
                healthConnectManager.deleteAllData()
            }
        }
    }

    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
        uiState = try {
            if (permissionsGranted.value) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    sealed class UiState {
        object Uninitialized : UiState()
        object Done : UiState()

        // A random UUID is used in each Error object to allow errors to be uniquely identified,
        // and recomposition won't result in multiple snackbars.
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}

class WelcomeScreenViewModelFactory(
    private val healthConnectManager: HealthConnectManager,
    private val exerciseSessionVM: ExerciseSessionViewModel,
    private val healthDataCenter: HealthDataCenter
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WelcomeScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WelcomeScreenViewModel(
                healthConnectManager = healthConnectManager,
                exerciseSessionVM = exerciseSessionVM,
                healthDataCenter = healthDataCenter
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}