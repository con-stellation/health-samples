package com.example.healthapp.service

import android.util.Log
import java.util.ArrayList

class DataEvaluationService {

    enum class DataIndices() {
        STRESS_SCORE, HRV
    }
    fun evaluateData(data: ArrayList<Int?>?) {
        Log.i("DataEvaluationService", "Received data: $data")

    }

    fun evaluateData(data: Int) {
        Log.i("DataEvaluationService", "Received exercise choice: $data")

    }
}
