package com.example.healthapp.service

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RealityCheck {
    private var vibrationJob: Job? = null
    @Inject
    lateinit var dataStoreManager: DataStoreManager

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

        vibrationJob = coroutineScope.launch(Dispatchers.Default) {
            val choice = dataStoreManager.readExerciseChoice()
            choice.collect {
                setExercise(it)
            }
            val repeatIndex = -1

            try {
                Log.i("RealityCheck", "Starting vibration")
                while(isActive) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            timings, amplitudes, repeatIndex))
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
