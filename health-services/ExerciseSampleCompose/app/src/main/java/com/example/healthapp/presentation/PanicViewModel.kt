package com.example.healthapp.presentation

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.runBlocking

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
                    exerciseClientManager.stopBreathingExercise()
                    delay(2000)
                    showInterventionDialog_Mutable.value = true
                    exerciseClientManager.announceDialog()
                }
                oldPanicDetectedValue = isCritical
            }.collect()
        }
    }

    fun confirmAssistance() {
        showInterventionDialog_Mutable.value = false
        dialogDismissedRecently = true
        viewModelScope.launch {
            val stressIndex = dataStoreManager.readStressData().first()
            phoneAFriend.sendMessageToContact(true, stressIndex)
        }
        // TODO hier ähnliche Logik wie in DismissDialog einbauen? Wenn zB jemand die Übung weitermachen will aber Beruhigung detektiert wird, sollte Dialogspam vermieden werden
        // zB if(!exerciseClientManager.heartRateCritical) { scope.launch{delay(5min oder so)... und heartRateCritical auf true?)
        viewModelScope.launch {
            runBlocking {
                exerciseClientManager.stopBreathingExercise()
            }
            Log.i("RealityCheck", "stopped previous vibration job.")

            exerciseClientManager.startBreathingExercise()
        }
        viewModelScope.launch {
            delay(60000)
            dialogDismissedRecently = false
            //exerciseClientManager.setCriticalHeartRateToPreviousState()
        }
    }

    fun onDismissDialog() {
        // Schließe den Dialog und setze den Zustand im Manager zurück.
        showInterventionDialog_Mutable.value = false
        dialogDismissedRecently = true
        viewModelScope.launch {
            exerciseClientManager.stopBreathingExercise()
        }

        viewModelScope.launch {
            delay(60000) // sodass immer korrekt Panik erkannt wird statt auf ungleiche oldPanic und isCritical Werte zu setzen (wäre inkorrektes Verhalten). Aber damit es nicht stört wird hier ein Timer eingebaut
            dialogDismissedRecently = false
            //exerciseClientManager.resetCriticalHeartRateState() // war vorher auskommentiert. Braucht man das hier? man will nach dem Delay ja wieder alles wie im normalen Betrieb aufnehmen. HrCritical sollte also durch eigene Detektion angepasst werden?
            //oder wegen oldPanicDetected value... dann lieber so:
            exerciseClientManager.setCriticalHeartRateToPreviousState() // dann wird der Dialog immer wieder nach 1min aktiviert tho..
        }
    }
}
