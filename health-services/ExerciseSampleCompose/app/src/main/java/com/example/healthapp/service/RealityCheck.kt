package com.example.healthapp.service

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RealityCheck @Inject constructor(private val dataStoreManager: DataStoreManager){
    private var vibrationJob: Job? = null

    var timings: LongArray = longArrayOf(
        4000, 7000, 8000)
    var amplitudes: IntArray = intArrayOf(
        160, 0, 50)

    fun setExercise(choice: String) {
        when(choice) {
            "4-7-8" -> {
                timings = longArrayOf(
                    4000, 7000, 8000)
                amplitudes = intArrayOf(
                    160, 0, 50)
            }
            "4-7-11" -> {
                timings = longArrayOf(
                    4000, 7000, 11000)
                amplitudes = intArrayOf(
                    160, 0, 50)
            }
            "6-3-6-3" -> {
                timings = longArrayOf(
                    6000, 3000, 6000, 3000)
                amplitudes = intArrayOf(
                    160, 0, 50, 0)
            }
        }
    }

     fun executeVibration(coroutineScope: CoroutineScope, vibrator: Vibrator): Job {
        stopVibrating(vibrator)
        Log.i("RealityCheck", "stopped previous vibration job.")
        vibrationJob = coroutineScope.launch(Dispatchers.Default) {
            val choice = dataStoreManager.readExerciseChoice().first()
            Log.i("RealityCheck", "ExerciseChoice: $choice")
            setExercise(choice)
            val repeatIndex = -1

            try {
                Log.i("RealityCheck", "Starting vibration")
                while(isActive) {

                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            timings, amplitudes, repeatIndex))
                    delay(timings.sum()+1)
                }
            }
            finally {
                Log.i("RealityCheck", "Stopping vibration")
                stopVibrating(vibrator)
            }
        }

         return vibrationJob!!
    }

    fun stopVibrating(vibrator: Vibrator) {
        if(vibrationJob?.isActive == true) {
            vibrationJob?.cancel()
        }
        vibrationJob = null
    }
}
