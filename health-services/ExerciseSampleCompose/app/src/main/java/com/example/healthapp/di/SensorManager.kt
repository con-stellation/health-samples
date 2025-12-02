package com.example.healthapp.di // Use your app's package structure

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideSensorManager(application: Application): SensorManager {
        return ContextCompat.getSystemService(application, SensorManager::class.java) as SensorManager
    }
}

