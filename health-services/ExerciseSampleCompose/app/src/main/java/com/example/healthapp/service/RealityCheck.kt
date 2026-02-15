package com.example.healthapp.service

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.healthapp.data.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    fun setExercise(choice: String) {
        when(choice) {
            "4-7-8" -> {
                timings = longArrayOf(
                    4000, 7000, 8000, 500)
                amplitudes = intArrayOf(
                    160, 0, 50, 0)
            }
            "4-7-11" -> {
                timings = longArrayOf(
                    4000, 7000, 11000, 500)
                amplitudes = intArrayOf(
                    160, 0, 50, 0)
            }
            "4-4-4-4" -> {
                timings = longArrayOf(
                    4000, 4000, 4000, 4000)
                amplitudes = intArrayOf(
                    160, 0, 50, 0)
            }
        }
    }

     fun executeVibration(coroutineScope: CoroutineScope, vibrator: Vibrator): Job {
        vibrationJob = coroutineScope.launch(Dispatchers.Default) {
            //stopVibrating(vibrator)
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

    fun executeDialogAnnouncement(vibrator: Vibrator) {
        var timings: LongArray = longArrayOf(500, 200, 500)
        var amplitudes: IntArray = intArrayOf(100, 0, 100)
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                timings, amplitudes, -1))
    }

    suspend fun stopVibrating(vibrator: Vibrator) {
        val vibrationJobCopy = vibrationJob
        if(vibrationJobCopy?.isActive == true) {
            vibrator.cancel()
            vibrationJobCopy.cancel()
            vibrationJobCopy.join()
        }
        vibrationJob = null
    }
}
