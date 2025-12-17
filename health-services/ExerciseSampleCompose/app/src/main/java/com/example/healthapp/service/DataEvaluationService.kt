package com.example.healthapp.service

import android.util.Log
import androidx.lifecycle.LifecycleService
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.coroutines.flow.first

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
    }


    suspend fun evaluateData(data: Int) {
        Log.i("DataEvaluationService", "Received exercise choice: $data")
        dataStoreManager.saveExerciseChoice(data)
    }
}
