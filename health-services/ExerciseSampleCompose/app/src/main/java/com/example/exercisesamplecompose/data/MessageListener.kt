package com.example.exercisesamplecompose.data

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MessageListener: WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/stress_request") {
            val payload = messageEvent.data.toString()
            Log.i("MessageListener", "Stress data received: $payload")
            //TODO start calculations or update globally known data here so it can be used in HR measurement code
        }
    }

}
