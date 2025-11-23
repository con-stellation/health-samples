package com.example.healthapp.service

import android.util.Log
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class DataEvaluationService {

    @Inject
    private lateinit var dataStoreManager: DataStoreManager

    suspend fun evaluateData(data: ArrayList<Int?>?) {
        Log.i("DataEvaluationService", "Received data: $data")
        dataStoreManager.saveHealthData(data)
    }

    suspend fun evaluateData(data: Int) {
        Log.i("DataEvaluationService", "Received exercise choice: $data")
        dataStoreManager.saveExerciseChoice(data)
    }
}
