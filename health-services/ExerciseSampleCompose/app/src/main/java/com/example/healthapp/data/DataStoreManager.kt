package com.example.healthapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DataIndices() {
    STRESS_SCORE, HRV, RESTING_HR
}

@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext private val context: Context){
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val EXERCISE_CHOICE = intPreferencesKey("exercise_choice")
    val HRV = intPreferencesKey("hrv")
    val STRESS_SCORE = intPreferencesKey("stress_score")
    val HR_MIN_THRESH = intPreferencesKey("hr_min_thresh")
    val HR_MAX_THRESH = intPreferencesKey("hr_max_thresh")
    val RESTING_HR = intPreferencesKey("resting_hr")

    fun readExerciseChoice(): Flow<Int> = context.dataStore.data.map { preferences ->
        val i = preferences[EXERCISE_CHOICE]
        i ?: 0
    }

    fun readThresholds(): Flow<ArrayList<Int?>> = context.dataStore.data.map { preferences ->
        val list = ArrayList<Int?>()
        list.add(preferences[HR_MIN_THRESH])
        list.add(preferences[HR_MAX_THRESH])
        list
    }


    suspend fun saveExerciseChoice(choice: Int) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[EXERCISE_CHOICE] = choice
            }
        }
    }

    suspend fun saveHealthData(data: ArrayList<Int?>?) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[HRV] = data?.get(DataIndices.HRV.ordinal) ?: 200
                preferences[STRESS_SCORE] = data?.get(DataIndices.STRESS_SCORE.ordinal) ?:75
                preferences[RESTING_HR] = data?.get(DataIndices.RESTING_HR.ordinal) ?:60

            }
        }
    }

    suspend fun saveThresholds(data: ArrayList<Int?>?) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[HR_MAX_THRESH] = data?.get(1) ?:100
                preferences[HR_MIN_THRESH] = data?.get(0) ?:60
            }
        }
    }

}
