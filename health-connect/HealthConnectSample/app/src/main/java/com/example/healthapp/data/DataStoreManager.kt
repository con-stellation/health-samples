package com.example.healthapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.also

class DataStoreManager (private val context: Context){
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val EXERCISE_CHOICE = intPreferencesKey("exercise_choice")
    val HRVAvg = intPreferencesKey("hrv_avg")
    val HRV = stringPreferencesKey("hrv_data")
    val CURRENT_HRV = intPreferencesKey("current_hrv")
    val EMERGENCY_NUM = stringPreferencesKey("emergency_num")
    val TAP_PREFERENCES = booleanPreferencesKey("tap_preferences")
    val TEAMBUILDING_PREFERENCES = booleanPreferencesKey("teambuilding_preferences")
    val EVENT_CONFIRMATION = booleanPreferencesKey("event_confirmation")
    val ANXIETY_SCORE = intPreferencesKey("anxiety_score")
    val USERINPUT_STATE = booleanPreferencesKey("userinput_state")



    fun readExerciseChoice(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[EXERCISE_CHOICE] ?: 0
    }

    fun readHrvAvg(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HRVAvg] ?: 47 // Default ist ein durch https://www.kubios.com/blog/heart-rate-variability-normal-range/ berechneter Durchschnittswert einer 24 jährigen Frau
    }

    fun readHrvData(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HRV] ?: "47, 47, 47, 47, 47, 47, 47"
    }

    fun readCurrentHrv(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_HRV] ?: 47
    }

    fun readEmergencyNumber(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[EMERGENCY_NUM] ?: ""
    }

    fun readTapPreferences(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TAP_PREFERENCES] ?: false
    }

    fun readTeambuildingPreferences(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TEAMBUILDING_PREFERENCES] ?: false
    }

    fun readEventConfirmation(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[EVENT_CONFIRMATION] ?: true
    }

    fun readAnxietyScore(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ANXIETY_SCORE] ?: 21 // schwerste Symptomatik nach GAD7
    }

    fun readInitUserInputState(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USERINPUT_STATE] ?: false
    }
    suspend fun saveExerciseChoice(choice: Int) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[EXERCISE_CHOICE] = choice
            }
        }
    }

    suspend fun saveCurrentHrv(hrv: Int) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[CURRENT_HRV] = hrv
            }
        }
    }
    suspend fun saveHrvData(avg: Int, data: String) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[HRV] = data
                preferences[HRVAvg] = avg
            }
        }
    }

    suspend fun saveEmergencyNumber(data: String) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[EMERGENCY_NUM] = data
            }
        }
    }

    suspend fun saveTapPreferences(data: Boolean) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[TAP_PREFERENCES] = data
            }
        }
    }

    suspend fun saveTeambuildingPreferences(data: Boolean) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[TEAMBUILDING_PREFERENCES] = data
            }
        }
    }

    suspend fun saveEventConfirmation(data: Boolean) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[EVENT_CONFIRMATION] = data
            }
        }
    }

    suspend fun saveAnxietyScore(data: Int) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[ANXIETY_SCORE] = data
            }
        }
    }

    suspend fun saveInitUserInputState(data: Boolean) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[USERINPUT_STATE] = data
            }
        }
    }
}
