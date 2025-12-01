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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RealityCheck @Inject constructor(private val dataStoreManager: DataStoreManager){
    private var vibrationJob: Job? = null

    var timings: LongArray = longArrayOf(
        4000, 7000, 8000)
    var amplitudes: IntArray = intArrayOf(
        160, 0, 50)


    enum class ExerciseChoice{
        fourSevenEight, fourSevenEleven, sixThreeSixThree
    }

    fun setExercise(choice: Int) {
        when(choice) {
            ExerciseChoice.fourSevenEight.ordinal -> {
                timings = longArrayOf(
                    4000, 7000, 8000)
                amplitudes = intArrayOf(
                    160, 0, 50)
            }
            ExerciseChoice.fourSevenEleven.ordinal -> {
                timings = longArrayOf(
                    4000, 7000, 11000)
                amplitudes = intArrayOf(
                    160, 0, 50)
            }
            ExerciseChoice.sixThreeSixThree.ordinal -> {
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
            val choice = dataStoreManager.readExerciseChoice()
            setExercise(choice.first())
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
