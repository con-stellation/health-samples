package com.example.exercisesamplecompose.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.lang.String

class MessageListener: WearableListenerService(), DataClient.OnDataChangedListener {
    private lateinit var dataClient: DataClient

    override fun onCreate() {
        super.onCreate()
        dataClient = Wearable.getDataClient(this)
        dataClient.addListener(this)
    }

    override fun onDataChanged(p0: DataEventBuffer) {
        Log.i("onMessageReceived", "Data update received with keys: ${p0.metadata?.keySet()}")

        p0.forEach { event ->
            Log.i("onMessageReceived", "Data update received with path: ${event.dataItem.uri.path}")
            Log.i("onMessageReceived", "Data update received with data: ${String(event.dataItem.data)}")
        }
        p0.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        dataClient.removeListener(this)
    }

}
