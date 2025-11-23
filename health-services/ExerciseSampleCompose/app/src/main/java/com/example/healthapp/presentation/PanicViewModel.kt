package com.example.healthapp.presentation

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

    init {
        viewModelScope.launch {
            exerciseClientManager.heartRateCritical.collect { isCritical ->
                isPanicDetected_Mutable.value = isCritical
            }
        }
    }

    fun confirmAssistance() {
        exerciseClientManager.startBreathingExercise()
        resetDialog()
    }

    fun onDismissDialog() {
        // Schließe den Dialog und setze den Zustand im Manager zurück.
        exerciseClientManager.updateHeartRateThreshold()
        resetDialog()
    }

    private fun resetDialog() {
        isPanicDetected_Mutable.value = false
        exerciseClientManager.resetCriticalHeartRateState()
    }

}
