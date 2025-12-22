package com.example.healthapp.service

import android.content.Context
import android.util.Log
import com.example.healthapp.data.DataStoreManager
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@Singleton
class PhoneAFriend @Inject constructor(@ApplicationContext private val context: Context, private val dataStoreManager: DataStoreManager) {

    private val dataClient by lazy {Wearable.getDataClient(context) }

    suspend fun sendMessageToContact(panicDetected: Boolean, stressIndex: Int) {
        try {
            val request = PutDataMapRequest.create("/phone_a_friend").apply {
                dataMap.putIntegerArrayList("stress_index", ArrayList(listOf(stressIndex)))
                dataMap.putBoolean("panic_detected", panicDetected)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d("sendMessageToWatch", "Data sent: ${request.data}. DataItem saved: ${result.data}")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d("sendMessageToWatch", "Saving DataItem failed: $exception")
        }

        Log.i("PhoneAFriend", "Sending message to contact. Panic? $panicDetected. Data: $stressIndex")
    }
}
