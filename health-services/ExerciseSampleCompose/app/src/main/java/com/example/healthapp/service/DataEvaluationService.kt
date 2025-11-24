package com.example.healthapp.service

import android.util.Log
import androidx.lifecycle.LifecycleService
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class DataEvaluationService : LifecycleService(){

    enum class DataIndices() {
        STRESS_SCORE, HRV, RESTING_HR
    }
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    suspend fun evaluateData(data: ArrayList<Int?>?) {
        Log.i("DataEvaluationService", "Received data: $data")
        dataStoreManager.saveHealthData(data)
        val restingHr = data?.get(DataIndices.RESTING_HR.ordinal) ?:70
        val thresh = dataStoreManager.readThresholds()
        val thresholds = ArrayList<Int?>()
        thresholds.add(restingHr + 20) // TODO auch anpassen je nach Minimum oder immer "aktuellen" nehmen?

        thresh.collect {
            it.get(1)?.let { it1 ->
                if(it1 > (restingHr + 40)) {
                    thresholds.add(restingHr + 40)
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
