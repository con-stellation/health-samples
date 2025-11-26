package com.example.healthapp.service

import android.util.Log
import androidx.lifecycle.LifecycleService
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class DataEvaluationService @Inject constructor(private val dataStoreManager: DataStoreManager): LifecycleService(){

    enum class DataIndices() {
        RESTING_HR, STRESS_SCORE
    }

    val phoneAFriend = PhoneAFriend()

    suspend fun evaluateData(data: ArrayList<Int?>?) {
        Log.i("DataEvaluationService", "Evaluating data: $data")
        dataStoreManager.saveHealthData(data)
        val stressscore = data?.get(DataIndices.STRESS_SCORE.ordinal) ?:50
        if(stressscore > 70) {
            phoneAFriend.sendMessageToContact(false, stressscore)
        }
        val restingHr = data?.get(DataIndices.RESTING_HR.ordinal) ?:70
        val thresh = dataStoreManager.readThresholds()
        val thresholds = ArrayList<Int?>()
        thresholds.add(restingHr + 15) // TODO auch anpassen je nach Minimum oder immer "aktuellen" nehmen? --> Der Aktuelle ist ja ein Durchschnittswert der letzten 7 Tage also ist das so schon okay

        thresh.collect {
            it.get(1)?.let { it1 ->
                if(it1 > (restingHr + 30)) {
                    thresholds.add(restingHr + 30)
                } else {
                    thresholds.add(it1)
                }
            }
        }

        dataStoreManager.saveThresholds(data)
    }

    suspend fun evaluateData(data: Int) {
        Log.i("DataEvaluationService", "Received exercise choice: $data")
        dataStoreManager.saveExerciseChoice(data)
    }
}
