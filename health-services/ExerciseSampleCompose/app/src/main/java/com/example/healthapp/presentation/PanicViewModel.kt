package com.example.healthapp.presentation

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.ExerciseClientManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    var showInterventionDialog = false

    init {
        viewModelScope.launch {
            exerciseClientManager.heartRateCritical.collect { isCritical ->
                Log.i("PanicViewModel", "Heart rate critical state changed: $isCritical")
                isPanicDetected_Mutable.value = isCritical
                if((!isCritical && oldPanicDetectedValue) || exerciseClientManager.tenMinutesPassed) {
                    showInterventionDialog = true
                }
                oldPanicDetectedValue = isCritical
            }

        }
    }

    fun confirmAssistance() {
        exerciseClientManager.tenMinutesPassed = false
        isPanicDetected_Mutable.value = false
        exerciseClientManager.startBreathingExercise()
    }

    fun onDismissDialog() {
        // Schließe den Dialog und setze den Zustand im Manager zurück.
        showInterventionDialog = false
        exerciseClientManager.tenMinutesPassed = false
        exerciseClientManager.stopBreathingExercise()
        exerciseClientManager.updateHeartRateThreshold()
        resetDialog()
    }

    private fun resetDialog() {
        isPanicDetected_Mutable.value = false
        exerciseClientManager.resetCriticalHeartRateState()
    }

}
