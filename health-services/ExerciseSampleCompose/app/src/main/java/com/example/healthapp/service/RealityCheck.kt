package com.example.healthapp.service

import android.os.VibrationEffect
import android.os.Vibrator
import com.example.healthapp.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RealityCheck {

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

    suspend fun executeVibration(vibrator: Vibrator) {
        val choice = dataStoreManager.readExerciseChoice()
        choice.collect {
            setExercise(it)
        }

        val repeatIndex = -1
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                timings, amplitudes, repeatIndex))

    }

    fun stopVibrating(vibrator: Vibrator) {
        vibrator.cancel()
    }
}
