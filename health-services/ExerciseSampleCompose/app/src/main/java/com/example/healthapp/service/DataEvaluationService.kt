package com.example.healthapp.service

import android.util.Log
import androidx.lifecycle.LifecycleService
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class DataEvaluationService @Inject constructor(private val dataStoreManager: DataStoreManager, private val phoneAFriend: PhoneAFriend): LifecycleService(){

    enum class DataIndices() {
        RESTING_HR, STRESS_SCORE
    }

    suspend fun evaluateData(data: ArrayList<Int?>?) {
        Log.i("DataEvaluationService", "Evaluating data: $data")
        val oldStressScore = dataStoreManager.readStressData().first()
        dataStoreManager.saveHealthData(data)
        val newStressScore = data?.get(DataIndices.STRESS_SCORE.ordinal) ?:50
        if((newStressScore > 0) && (newStressScore > oldStressScore)) {
            phoneAFriend.sendMessageToContact(false, newStressScore)
        }
    }

    suspend fun evaluateData(data: String) {
        Log.i("DataEvaluationService", "Received exercise choice: $data")
        dataStoreManager.saveExerciseChoice(data)
    }
}
