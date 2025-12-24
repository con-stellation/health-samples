package com.example.healthapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.also
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DataIndices() {
    RESTING_HR, STRESS_SCORE
}

@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext private val context: Context){
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val EXERCISE_CHOICE = stringPreferencesKey("exercise_choice")
    val HRV = intPreferencesKey("hrv")
    val STRESS_SCORE = intPreferencesKey("stress_score")
    val RESTING_HR = intPreferencesKey("resting_hr")

    fun readExerciseChoice(): Flow<String> = context.dataStore.data.map { preferences ->
        val i = preferences[EXERCISE_CHOICE]
        i ?: ""
    }

    suspend fun saveExerciseChoice(choice: String) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[EXERCISE_CHOICE] = choice
            }
        }
    }

    suspend fun saveHealthData(data: ArrayList<Int?>?) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                //preferences[HRV] = data?.get(DataIndices.HRV.ordinal) ?: 200
                preferences[RESTING_HR] = data?.get(DataIndices.RESTING_HR.ordinal) ?:60
                preferences[STRESS_SCORE] = data?.get(DataIndices.STRESS_SCORE.ordinal) ?:50
            }
        }
    }

    fun readStressData(): Flow<Int> = context.dataStore.data.map { preferences ->
        val i = preferences[STRESS_SCORE]
        i ?: 0
    }
}
