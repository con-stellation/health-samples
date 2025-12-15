/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthapp.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.concurrent.futures.await
import android.os.Vibrator
import android.util.Log
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseTypeCapabilities
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.data.WarmUpConfig
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.prepareExercise
import androidx.health.services.client.resumeExercise
import androidx.wear.remote.interactions.RemoteActivityHelper
import android.os.SystemClock
import com.example.healthapp.service.ExerciseLogger
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import com.example.healthapp.presentation.exercise.DynamicTimeWarping
import com.example.healthapp.service.RealityCheck
import com.google.android.gms.tasks.Tasks
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Entry point for [HealthServicesClient] APIs, wrapping them in coroutine-friendly APIs.
 */
@SuppressLint("RestrictedApi")
@Singleton
class ExerciseClientManager
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    healthServicesClient: HealthServicesClient,
    private val logger: ExerciseLogger,
    private val vibrator: Vibrator,
    private val realityCheck: RealityCheck,
    private val sensorManager: SensorManager
) {
    val exerciseClient: ExerciseClient = healthServicesClient.exerciseClient
    var breathingExerciseJob: Job? = null
    private val managerScope = CoroutineScope(Dispatchers.Default)
    val prePanicTemplate = DtwTemplates.Companion.prePanicSmoothedTemplate()
    val postPanicTemplate = DtwTemplates.Companion.postPanicSmoothedTemplate()
    val restingHrTemplate = DtwTemplates.Companion.restingHrTemplateSmoothedS11()
    private val heartRateCriticalMutableFlow = MutableStateFlow(false)
    val heartRateCritical = heartRateCriticalMutableFlow.asStateFlow()
    var tenMinutesPassed = false
    private val hrDtwWindow = mutableListOf<Double>()
    private var heartRateSensor: Sensor?=null

    suspend fun getExerciseCapabilities(): ExerciseTypeCapabilities? {
        val capabilities = exerciseClient.getCapabilities()

        return if (ExerciseType.RUNNING in capabilities.supportedExerciseTypes) {
            capabilities.getExerciseTypeCapabilities(ExerciseType.RUNNING)
        } else {
            null
        }
    }

    /***
     * Note: don't call this method from outside of ExerciseService.kt
     * when acquiring calories or distance.
     */
    suspend fun prepareExercise() {
        logger.log("Preparing an exercise")
        val warmUpConfig =
            WarmUpConfig(
                exerciseType = ExerciseType.RUNNING,
                dataTypes = setOf(DataType.HEART_RATE_BPM, DataType.LOCATION)
            )
        try {
            exerciseClient.prepareExercise(warmUpConfig)
        } catch (e: Exception) {
            logger.log("Prepare exercise failed - ${e.message}")
        }
    }

    suspend fun endExercise() {
        logger.log("Ending exercise")
        stopHrMonitoring()
        exerciseClient.endExercise()
    }

    suspend fun pauseExercise() {
        logger.log("Pausing exercise")
        exerciseClient.pauseExercise()
    }

    suspend fun resumeExercise() {
        logger.log("Resuming exercise")
        exerciseClient.resumeExercise()
    }

    fun startBreathingExercise() {
        if (breathingExerciseJob?.isActive != true) {
            Log.i("ExerciseClientManager", "Starting breathing exercise")
            //val startTime = System.currentTimeMillis()
            managerScope.launch {
                delay(60000 * 10) // wait 10 min and ask again
                // TODO how to signal panicViewModel that we need to show dialog... via tenMinutesPassed variable?
                Log.i("ExerciseClientManager", "10 minutes have passed...")
                tenMinutesPassed = true
            }
            breathingExerciseJob = realityCheck.executeVibration(managerScope, vibrator)

        }
    }

    fun stopBreathingExercise() {
        if (breathingExerciseJob?.isActive == true) {
            Log.i("ExerciseClientManager", "Stopping breathing exercise")
                    realityCheck.stopVibrating(vibrator)
            breathingExerciseJob?.cancel()
            breathingExerciseJob = null
        }
    }

    fun resetCriticalHeartRateState() {
        heartRateCriticalMutableFlow.value = false
    }
    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            Log.i("ExerciseClientManager", "Sensor accuracy changed: $p1")
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
                val heartRate = event.values[0]

                if (heartRate > 0) { // Ignoriere ungültige Werte

                    Log.i(
                        "ExerciseClientManager",
                        "Heart rate via Sensor: $heartRate, eventTime: ${event.timestamp}"
                    )

                    // 1. Neuen Punkt zum DTW-Fenster hinzufügen
                    while(hrDtwWindow.size >= 900) {
                        hrDtwWindow.removeAt(0)
                    }

                    hrDtwWindow.add(heartRate.toDouble())
                    Log.i("ExerciseClientManager", "HR Windowsize: ${hrDtwWindow.size}")

                    // 3. DTW-Analyse starten
                    if (hrDtwWindow.size > 780) {
                        Log.i("ExerciseClientManager", "Got at least 13 minutes of Data. Size: ${hrDtwWindow.size}. Starting DTW analysis")
                        runDtwAnalysis()
                    }
                }
            }
        }

        var dtwDistance: Double = 0.0
        var calmDtwDistance: Double = Double.MAX_VALUE

        private fun runDtwAnalysis() {
            managerScope.launch {
                // Extrahiere nur die HR-Werte aus deinem Fenster
                val liveHrValues = hrDtwWindow.toDoubleArray()
                // WICHTIG: Interpoliere die Live-Daten, damit sie die gleiche Länge wie das Template haben!
                // DTW kann mit unterschiedlich langen Reihen umgehen, aber für den Vergleich ist gleiche Länge besser.
                val interpolatedLiveValues = interpolate(liveHrValues, prePanicTemplate.size)

                // DTW-Distanz berechnen
                if(breathingExerciseJob?.isActive == true) {
                    calmDtwDistance = DynamicTimeWarping.calculateDistance(postPanicTemplate, interpolatedLiveValues)
                }
                 dtwDistance = DynamicTimeWarping.calculateDistance(prePanicTemplate, interpolatedLiveValues)

                Log.d("DTW_Analysis", "DTW Distance: $dtwDistance")

                // Definiere einen Schwellenwert für die Distanz.
                // Wenn die Distanz UNTER diesem Wert liegt, sind die Kurven sehr ähnlich.
                // Dieser Wert muss durch Experimentieren gefunden werden!
                val dtwThreshold = 1000.0 // Fiktiver Startwert!

                if (dtwDistance < dtwThreshold || calmDtwDistance < dtwThreshold) {
                    Log.w("DTW_Analysis", "PANIC ATTACK PATTERNS panic Distance: $dtwDistance, calm Distance: $calmDtwDistance")

                    if(dtwDistance >= calmDtwDistance) {
                        heartRateCriticalMutableFlow.value = true
                    } else {
                        heartRateCriticalMutableFlow.value = false
                    }
                }
            }
        }

        // Eine einfache Interpolations-Hilfsfunktion
        private fun interpolate(source: DoubleArray, newSize: Int): DoubleArray {
            if (source.size == newSize) return source
            val result = DoubleArray(newSize)
            val factor = (source.size - 1).toDouble() / (newSize - 1)
            for (i in 0 until newSize) {
                val srcIndex = i * factor
                val i0 = srcIndex.toInt()
                val i1 = min(i0 + 1, source.size - 1)
                if (i0 == i1) {
                    result[i] = source[i0]
                } else {
                    val w1 = srcIndex - i0
                    val w0 = 1.0 - w1
                    result[i] = w0 * source[i0] + w1 * source[i1]
                }
            }
            return result
        }
    }

    suspend fun startHrMonitoring() {
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heartRateSensor == null) {
            val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
            Log.e("ExerciseClientManager", "No heartrate sensor detected. Sensors: $sensorList")
            return
        }

        sensorManager.registerListener(sensorListener, heartRateSensor, 1_000_000)

        // hier wird die Companionapp gestartet (Daten werden danach empfangen)
        val exec : Executor = Executors.newSingleThreadExecutor()
        val connectedNodes = Tasks.await(Wearable.getNodeClient(applicationContext).connectedNodes)
        Log.i("ExerciseClientManager", "Connected nodes: $connectedNodes. Trying to start remote companion.")
        if(!connectedNodes.isEmpty()){
            val remoteActivityHelper = RemoteActivityHelper(applicationContext, exec)
            val remoteResult = remoteActivityHelper.startRemoteActivity(
                Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE).setData("companionapp://sms92".toUri()),
                connectedNodes[0].id
            ).await()

            try {
                Log.i("WearOSRemote", "Remote Activity Started?")
            } catch (e: Exception) {
                Log.e("WearOSRemote", "Fehler beim Starten der Remote Activity", e)
            }
        } else {
            Log.i("ExerciseClientService", "No connected devices detected. Cannot launch remote Companion.")
        }
    }

    private fun stopHrMonitoring() {
        sensorManager.unregisterListener(sensorListener)
        hrDtwWindow.clear()
        Log.i("DTW_Sensor", "SensorListener deregistriert.")
    }
}

private fun logMetrics(metrics: DataPointContainer) {
    metrics.getData(DataType.HEART_RATE_BPM).forEach { dataPoint ->
        val bpm = dataPoint.value
        val timeStamp = dataPoint.timeDurationFromBoot // Oder eine andere Zeitangabe
        Log.d("ExerciseService_HR_Background", "Measured Heartrate (im Service): $bpm BPM, Zeitstempel: $timeStamp")

    }
}
