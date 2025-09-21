package com.example.exercisesamplecompose.data

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MessageListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = String(messageEvent.data, Charsets.UTF_8)

        if (path == "/stress_request") {
            Log.i("MessageListener", "Stress data received: $payload")
            // TODO: hier globale Variable setzen oder Service antriggern
        } else {
            Log.i("MessageListener", "Received message on path=$path with payload=$payload")
        }
    }
}
