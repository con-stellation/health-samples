package com.example.healthapp.presentation

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.DataStoreManager
import com.example.healthapp.data.ExerciseClientManager
import com.example.healthapp.service.PhoneAFriend
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class PanicViewModel
@Inject
constructor(
    private val exerciseClientManager: ExerciseClientManager, private val phoneAFriend: PhoneAFriend, private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private var dialogDismissedRecently = false
    var oldPanicDetectedValue = false
    private val showInterventionDialog_Mutable = MutableStateFlow(false)
    var showInterventionDialog = showInterventionDialog_Mutable.asStateFlow()

    init {
        viewModelScope.launch {
            combine(exerciseClientManager.heartRateCritical, exerciseClientManager.tenMinutesPassed, exerciseClientManager.monitoringEnded){
                    isCritical, tenMinutesPassed, monitoringEnded ->
                Log.i("PanicViewModel", "Heart rate critical state changed: $isCritical")
                if(monitoringEnded) {
                    //resetting the values used for showing the dialog incase states are saved from one monitoring session into the other
                    showInterventionDialog_Mutable.value = false
                    exerciseClientManager.resetCriticalHeartRateState()
                } else if((!isCritical && oldPanicDetectedValue && !dialogDismissedRecently) || tenMinutesPassed || (isCritical && !oldPanicDetectedValue && !dialogDismissedRecently)) {
                    Log.i("PanicViewModel", "isCritical: $isCritical, oldPanicDetectedValue: $oldPanicDetectedValue, tenMinutesPassed: $tenMinutesPassed")
                    showInterventionDialog_Mutable.value = true
                }
                oldPanicDetectedValue = isCritical
            }.collect()
        }
    }

    fun confirmAssistance() {
        showInterventionDialog_Mutable.value = false
        dialogDismissedRecently = false
        viewModelScope.launch {
            val stressIndex = dataStoreManager.readStressData().first()
            phoneAFriend.sendMessageToContact(true, stressIndex)
        }
        // TODO hier ähnliche Logik wie in DismissDialog einbauen? Wenn zB jemand die Übung weitermachen will aber Beruhigung detektiert wird, sollte Dialogspam vermieden werden
        // zB if(!exerciseClientManager.heartRateCritical) { scope.launch{delay(5min oder so)... und heartRateCritical auf true?)
        exerciseClientManager.startBreathingExercise()
    }

    fun onDismissDialog() {
        // Schließe den Dialog und setze den Zustand im Manager zurück.
        showInterventionDialog_Mutable.value = false
        dialogDismissedRecently = true
        exerciseClientManager.stopBreathingExercise()

        viewModelScope.launch {
            delay(60000) // sodass immer korrekt Panik erkannt wird statt auf ungleiche oldPanic und isCritical Werte zu setzen (wäre inkorrektes Verhalten). Aber damit es nicht stört wird hier ein Timer eingebaut
            dialogDismissedRecently = false
            exerciseClientManager.resetCriticalHeartRateState() // war vorher auskommentiert
        }
    }

}
