package com.example.exercisesamplecompose.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule {

    // In HealthConnectModule
    @Provides
    @Singleton
    fun provideHealthConnectClient(@ApplicationContext context: Context): HealthConnectClient {
        // Diagnostische Prüfung
        val providerPackageName = "com.google.android.wearable.healthservices" // Für Wear OS
        // Alternativ, für Android U+ sollte auch ohne spezifischen Provider gehen:
        // val availability = HealthConnectClient.getSdkStatus(context) // Prüft gegen den Standard-Systemprovider

        val availability = HealthConnectClient.getSdkStatus(context, providerPackageName)
        Log.d("HealthConnectModule", "Health Connect SDK Status für $providerPackageName: $availability")

        if (availability == HealthConnectClient.SDK_UNAVAILABLE) {
            Log.e("HealthConnectModule", "Health Connect SDK ist laut Statusprüfung NICHT VERFÜGBAR.")
            // Hier sollten Sie nicht einfach den Client erstellen, da es fehlschlagen wird.
            // Werfen Sie eine spezifischere Exception oder geben Sie einen Null-Client zurück und behandeln Sie das.
            throw IllegalStateException("Health Connect SDK ist nicht verfügbar auf diesem Gerät/Profil.")
        } else if (availability == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Log.e("HealthConnectModule", "Health Connect Provider benötigt ein Update.")
            throw IllegalStateException("Health Connect Provider benötigt ein Update.")
        }

        // Wenn verfügbar, dann versuchen, den Client zu erstellen
        Log.d("HealthConnectModule", "Versuche HealthConnectClient.getOrCreate(context) aufzurufen...")
        try {
            return HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e("HealthConnectModule", "EXCEPTION bei HealthConnectClient.getOrCreate: ${e.message}", e)
            throw e // Die ursprüngliche Exception weiterwerfen, damit der Crash weiterhin sichtbar ist
        }
    }

}
