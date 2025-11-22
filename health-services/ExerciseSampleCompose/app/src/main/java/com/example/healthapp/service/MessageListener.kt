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
    val PATH = "/stress_score"

    private val messageClient by lazy { Wearable.getMessageClient(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { dataEvent ->
            val uri = dataEvent.dataItem.uri
            when (uri.path) {
                PATH -> {
                    // Retrieve the count. This must be done synchronously: dataEvents is likely to
                    // be stale/invalid if accessed from the new coroutine created by scope.launch.
                    val dataMapItem = DataMapItem.fromDataItem(dataEvent.dataItem)
                    val score = dataMapItem.dataMap.getInt("streess_score", 0)
                    Log.d("MessageListener", "Score received: $score")
                    scope.launch {
                        try {
                            val nodeId = uri.host!!
                            val payload = uri.toString().toByteArray()
                            messageClient.sendMessage(
                                nodeId,
                                "Acknowledge",
                                payload
                            ).await()

                            Log.d("MessageListener", "Message sent successfully")
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (exception: Exception) {
                            Log.d("MessageListener", "Message failed")
                        }
                    }
                }
            }
        }
    }

}
