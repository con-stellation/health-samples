package com.example.healthapp.presentation

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.ExerciseClientManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class PanicViewModel
@Inject
constructor(
    private val exerciseClientManager: ExerciseClientManager
) : ViewModel() {

    private val isPanicDetected_Mutable = MutableStateFlow(false)
    val isPanicDetected = isPanicDetected_Mutable.asStateFlow()
    var oldPanicDetectedValue = false
    private val showInterventionDialog_Mutable = MutableStateFlow(false)
    var showInterventionDialog = showInterventionDialog_Mutable.asStateFlow()
    init {
        viewModelScope.launch {
            combine(exerciseClientManager.heartRateCritical, exerciseClientManager.tenMinutesPassed, exerciseClientManager.monitoringEnded){
                    isCritical, tenMinutesPassed, monitoringEnded ->
                Log.i("PanicViewModel", "Heart rate critical state changed: $isCritical")
                isPanicDetected_Mutable.value = (isCritical && !oldPanicDetectedValue)
                if((!isCritical && oldPanicDetectedValue) || tenMinutesPassed) {
                    Log.i("PanicViewModel", "isCritical: $isCritical, oldPanicDetectedValue: $oldPanicDetectedValue, tenMinutesPassed: $tenMinutesPassed")
                    showInterventionDialog_Mutable.value = true
                }
                if(monitoringEnded) {
                    //resetting the values used for showing the dialog incase states are saved from one monitoring session into the other
                    isPanicDetected_Mutable.value = false
                    showInterventionDialog_Mutable.value = false
                    exerciseClientManager.resetCriticalHeartRateState()
                }
                oldPanicDetectedValue = isCritical
            }.collect()
        }
    }

    fun confirmAssistance() {
        isPanicDetected_Mutable.value = false
        showInterventionDialog_Mutable.value = false
        exerciseClientManager.startBreathingExercise()
    }

    fun onDismissDialog() {
        // Schließe den Dialog und setze den Zustand im Manager zurück.
        showInterventionDialog_Mutable.value = false
        isPanicDetected_Mutable.value = false
        exerciseClientManager.stopBreathingExercise()
        resetDialog()
    }

    private fun resetDialog() {
        isPanicDetected_Mutable.value = false
        exerciseClientManager.resetCriticalHeartRateState()
    }

}
