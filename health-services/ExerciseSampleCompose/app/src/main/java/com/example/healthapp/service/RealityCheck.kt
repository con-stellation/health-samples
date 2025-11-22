package com.example.healthapp.service

import android.os.VibrationEffect
import android.os.Vibrator

class RealityCheck {

    var timings: LongArray = longArrayOf(
        4000, 7000, 8000)
    var amplitudes: IntArray = intArrayOf(
        160, 0, 50)


    enum class ExerciseChoice{
        fourSevenEight, fourSevenEleven, sixThreeSixThree
    }

    fun setExercise(choice: ExerciseChoice) {
        when(choice) {
            ExerciseChoice.fourSevenEight -> {
                timings = longArrayOf(
                    4000, 7000, 8000)
                amplitudes = intArrayOf(
                    160, 0, 50)
            }
            ExerciseChoice.fourSevenEleven -> {
                timings = longArrayOf(
                    4000, 7000, 11000)
                amplitudes = intArrayOf(
                    160, 0, 50)
            }
            ExerciseChoice.sixThreeSixThree -> {
                timings = longArrayOf(
                    6000, 3000, 6000, 3000)
                amplitudes = intArrayOf(
                    160, 0, 50, 0)
            }
        }
    }

    fun executeVibration(vibrator: Vibrator) {

        val repeatIndex = -1
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                timings, amplitudes, repeatIndex))

    }

    fun stopVibrating(vibrator: Vibrator) {
        vibrator.cancel()
    }
}
