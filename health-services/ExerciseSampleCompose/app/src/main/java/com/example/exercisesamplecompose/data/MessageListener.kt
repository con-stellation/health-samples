package com.example.exercisesamplecompose.data

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MessageListener: MessageClient.OnMessageReceivedListener {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.i("onMessageReceived", "Event received: $messageEvent on path: ${messageEvent.path}")
        val payload = messageEvent.data.toString()
        if (messageEvent.path == "/stress_request") {
            Log.i("onMessageReceived", "Stress data received: $payload")
            //TODO start calculations or update globally known data here so it can be used in HR measurement code
        } else {
            val path = messageEvent.path
            Log.i("onMessageReceived", "Received data from another path $path with payload $payload")
        }
    }

}
