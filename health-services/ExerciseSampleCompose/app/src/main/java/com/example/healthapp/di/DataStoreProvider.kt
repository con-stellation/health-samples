package com.example.healthapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Definiert einen globalen Delegaten für den DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class) // Macht die Instanz als Singleton in der ganzen App verfügbar
object DataStoreProvider {

    @Provides
    @Singleton // Stellt sicher, dass es nur EINE Instanz des DataStore gibt
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        // Verwendet den oben definierten Delegaten, um die Singleton-Instanz zurückzugeben
        return context.dataStore
    }
}

