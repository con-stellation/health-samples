package com.example.healthapp.service

import android.util.Log
import com.example.healthapp.data.DataStoreManager
import javax.inject.Inject

class PhoneAFriend {
    @Inject
    lateinit var dataStoreManager: DataStoreManager


    fun sendMessageToContact(panicDetected: Boolean, data: Int) {
        // TODO
        Log.i("PhoneAFriend", "Sending message to contact. Panic? $panicDetected. Data: $data")
    }
}
