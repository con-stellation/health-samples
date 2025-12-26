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

    suspend fun evaluateStressData(data: Int) {
        Log.i("DataEvaluationService", "Evaluating data: $data")
        dataStoreManager.saveHealthData(data)
    }

    suspend fun evaluateData(data: String) {
        Log.i("DataEvaluationService", "Received exercise choice: $data")
        dataStoreManager.saveExerciseChoice(data)
    }
}
