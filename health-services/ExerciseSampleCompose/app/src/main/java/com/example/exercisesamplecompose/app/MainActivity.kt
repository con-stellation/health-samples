/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.exercisesamplecompose.app

import android.Manifest
import android.health.connect.HealthPermissions
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.isEmpty
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.exercisesamplecompose.presentation.ExerciseSampleApp
import com.example.exercisesamplecompose.presentation.exercise.ExerciseViewModel
import com.example.exercisesamplecompose.presentation.preparing.PreparingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private lateinit var navController: NavHostController
    private val exerciseViewModel by viewModels<ExerciseViewModel>()
    private val preparingViewModel by viewModels<PreparingViewModel>()
    @Inject // Beispiel: Injizieren Sie den HealthConnectClient, wenn Sie Hilt verwenden
    lateinit var healthConnectClient: HealthConnectClient
    // Register the permissions callback
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // All permissions granted, proceed with exercise
            checkAndRequestHealthConnectPermissions()
        }
    }

    private fun checkAndRequestHealthConnectPermissions() {
        val neededHcPermissions = getNeededHealthConnectPermissions()
        if (neededHcPermissions.isEmpty()) {
            Log.d("Permissions", "Keine Health Connect Berechtigungen zum Anfordern definiert.")
            // Ggf. Übung ohne HC starten oder UI anpassen
            exerciseViewModel.startExercise() // Beispiel: Übung ohne HC starten
            return
        }

        // Stellen Sie sicher, dass der healthConnectClient initialisiert ist (z.B. über Hilt)
        // Dieser Check ist wichtig, falls der Client noch nicht bereit ist (sollte aber durch Hilt sein)
        if (!::healthConnectClient.isInitialized) {
            Log.e("Permissions", "HealthConnectClient ist nicht initialisiert!")
            // Fehlerbehandlung oder Nutzer informieren
            return
        }

        lifecycleScope.launch{ // permissionController Methoden sind oft suspend
            try {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                if (granted.containsAll(neededHcPermissions)) {
                    Log.d("Permissions", "Alle benötigten Health Connect Berechtigungen bereits erteilt.")
                    exerciseViewModel.startExercise()
                } else {
                    Log.d("Permissions", "Fordere Health Connect Berechtigungen an.")
                    Log.d("Permissions", "Benötigt: $neededHcPermissions")
                    Log.d("Permissions", "Bereits erteilt: $granted")
                    requestHealthConnectPermissionsLauncher.launch(neededHcPermissions) // Startet die Health Connect UI
                }
            } catch (e: Exception) {
                Log.e("Permissions", "Fehler bei der Überprüfung/Anforderung von HC-Berechtigungen", e)
                // Fehlerbehandlung, z.B. wenn Health Connect nicht verfügbar ist (sollte auf Wear OS 4+ nicht der Fall sein, aber...)
                // Hier könnte die ursprüngliche "UnsupportedOperationException" wieder auftreten, wenn HC Client nicht erstellt werden kann.
            }
        }
    }
        private val requestHealthConnectPermissionsLauncher =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { grantedPermissions ->
            // grantedPermissions ist ein Set<String> der ERTEILTEN Health Connect Berechtigungen
            val neededHcPermissions = getNeededHealthConnectPermissions() // Holen Sie sich die erneut, um sicher zu vergleichen
            if (grantedPermissions.containsAll(neededHcPermissions)) {
                Log.d("Permissions", "Alle benötigten Health Connect Berechtigungen erteilt.")
                exerciseViewModel.startExercise()
            } else {
                Log.w("Permissions", "Nicht alle benötigten Health Connect Berechtigungen erteilt.")
                Log.d("Permissions", "Benötigt: $neededHcPermissions")
                Log.d("Permissions", "Erteilt: $grantedPermissions")
                // Nutzer informieren
            }
        }

    private fun getNeededHealthConnectPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class), // Fügen Sie auch Schreibberechtigungen hinzu!
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getWritePermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getWritePermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class)
            // Fügen Sie hier ALLE benötigten Lese- UND Schreibberechtigungen für Health Connect hinzu
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var pendingNavigation = true

        splash.setKeepOnScreenCondition { pendingNavigation }

        super.onCreate(savedInstanceState)
        // Request permissions when activity is created
        requestPermissions.launch(PreparingViewModel.permissions.toTypedArray())
        setContent {
            navController = rememberSwipeDismissableNavController()

            ExerciseSampleApp(
                navController,
                onFinishActivity = { this.finish() }
            )

            LaunchedEffect(Unit) {
                prepareIfNoExercise()
                pendingNavigation = false
            }
        }
    }

    private suspend fun prepareIfNoExercise() {
        /** Check if we have an active exercise. If true, set our destination as the
         * Exercise Screen. If false, route to preparing a new exercise. **/
        val isRegularLaunch =
            navController.currentDestination?.route == Screen.Exercise.route
        if (isRegularLaunch && !exerciseViewModel.isExerciseInProgress()) {
            navController.navigate(Screen.PreparingExercise.route)
        }
    }
}
