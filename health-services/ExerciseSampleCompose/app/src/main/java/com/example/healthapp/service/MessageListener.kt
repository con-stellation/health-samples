package com.example.healthapp.service

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MessageListener: WearableListenerService(), DataClient.OnDataChangedListener {
    private lateinit var dataClient: DataClient
    val STRESS_PATH = "/stress_score"
    val EXERCISE_PATH = "/exercise_choice"
    val dataEvaluation = DataEvaluationService()

    private val messageClient by lazy { Wearable.getMessageClient(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { dataEvent ->
            val uri = dataEvent.dataItem.uri
            when (uri.path) {
                STRESS_PATH -> {
                    val dataMapItem = DataMapItem.fromDataItem(dataEvent.dataItem)
                    val companionData = dataMapItem.dataMap.getIntegerArrayList("stress_score")
                    Log.d("MessageListener StressScore", "Score received: $companionData")
                    scope.launch {
                        try {
                            val nodeId = uri.host!!
                            val payload = uri.toString().toByteArray()
                            messageClient.sendMessage(
                                nodeId,
                                "Acknowledge",
                                payload
                            ).await()

                            Log.d("MessageListener StressScore", "Message sent successfully")
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (exception: Exception) {
                            Log.d("MessageListener StressScore", "Message failed")
                        }
                        dataEvaluation.evaluateData(companionData)
                    }
                }
                EXERCISE_PATH -> {
                    val dataMapItem = DataMapItem.fromDataItem(dataEvent.dataItem)
                    val companionData = dataMapItem.dataMap.getInt("exercise_choice")
                    Log.d("MessageListener ExerciseChoice", "Exercise choice updated: $companionData")
                    scope.launch {
                        try {
                            val nodeId = uri.host!!
                            val payload = uri.toString().toByteArray()
                            messageClient.sendMessage(
                                nodeId,
                                "Acknowledge",
                                payload
                            ).await()

                            Log.d("MessageListener ExerciseChoice", "Message sent successfully")
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (exception: Exception) {
                            Log.d("MessageListener ExerciseChoice", "Message failed")
                        }
                        dataEvaluation.evaluateData(companionData)
                    }
                }
            }
        }
    }

}
