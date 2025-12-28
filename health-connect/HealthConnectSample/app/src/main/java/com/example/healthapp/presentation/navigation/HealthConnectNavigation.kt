/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthapp.presentation.navigation

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.healthapp.data.HealthConnectManager
import com.example.healthapp.data.HealthDataCenter
import com.example.healthapp.presentation.screen.SettingsScreen
import com.example.healthapp.presentation.screen.WelcomeScreen
import com.example.healthapp.presentation.screen.WelcomeScreenViewModel
import com.example.healthapp.presentation.screen.WelcomeScreenViewModelFactory
import com.example.healthapp.presentation.screen.changes.DifferentialChangesScreen
import com.example.healthapp.presentation.screen.changes.DifferentialChangesViewModel
import com.example.healthapp.presentation.screen.changes.DifferentialChangesViewModelFactory
import com.example.healthapp.presentation.screen.exercisesession.ExerciseSessionScreen
import com.example.healthapp.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.example.healthapp.presentation.screen.exercisesession.ExerciseSessionViewModelFactory
import com.example.healthapp.presentation.screen.exercisesessiondetail.ExerciseSessionDetailScreen
import com.example.healthapp.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModel
import com.example.healthapp.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModelFactory
import com.example.healthapp.presentation.screen.privacypolicy.PrivacyPolicyScreen
import com.example.healthapp.presentation.screen.recordlist.RecordListScreen
import com.example.healthapp.presentation.screen.recordlist.RecordListScreenViewModel
import com.example.healthapp.presentation.screen.recordlist.RecordListViewModelFactory
import com.example.healthapp.presentation.screen.recordlist.RecordType
import com.example.healthapp.presentation.screen.recordlist.SeriesRecordsType
import com.example.healthapp.presentation.screen.sleepsession.SleepSessionScreen
import com.example.healthapp.presentation.screen.sleepsession.SleepSessionViewModel
import com.example.healthapp.presentation.screen.sleepsession.SleepSessionViewModelFactory
import com.example.healthapp.presentation.screen.userinputs.AppointmentInfoScreen
import com.example.healthapp.presentation.screen.userinputs.AppointmentInfoViewModel
import com.example.healthapp.presentation.screen.userinputs.AppointmentInfoViewModelFactory
import com.example.healthapp.presentation.screen.userinputs.GAD7Screen
import com.example.healthapp.presentation.screen.userinputs.GAD7ViewModel
import com.example.healthapp.presentation.screen.userinputs.GAD7ViewModelFactory
import com.example.healthapp.presentation.screen.userinputs.PhoneAFriendScreen
import com.example.healthapp.presentation.screen.userinputs.PhoneAFriendViewModel
import com.example.healthapp.presentation.screen.userinputs.PhoneAFriendViewModelFactory
import com.example.healthapp.showExceptionSnackbar
import kotlinx.coroutines.launch
import android.Manifest
import com.example.healthapp.presentation.screen.userinputs.ExerciseChoiceScreen
import com.example.healthapp.presentation.screen.userinputs.ExerciseChoiceViewModel
import com.example.healthapp.presentation.screen.userinputs.ExerciseChoiceViewModelFactory

/**
 * Provides the navigation in the app.
 */
@Composable
fun HealthConnectNavigation(
    navController: NavHostController,
    healthConnectManager: HealthConnectManager,
    scaffoldState: ScaffoldState
) {

    val exerciseSessionViewModel: ExerciseSessionViewModel = viewModel(
        factory = ExerciseSessionViewModelFactory(
            healthConnectManager = healthConnectManager
        )
    )
    val sleepSessionViewModel: SleepSessionViewModel = viewModel(
        factory = SleepSessionViewModelFactory(
            healthConnectManager = healthConnectManager
        )
    )
    val scope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = Screen.WelcomeScreen.route) {
        val availability by healthConnectManager.availability
        composable(Screen.WelcomeScreen.route) {

            val healthDataCenter = HealthDataCenter(healthConnectManager)
            val viewModel: WelcomeScreenViewModel = viewModel(
                factory = WelcomeScreenViewModelFactory(
                    healthConnectManager = healthConnectManager,
                    exerciseSessionVM = exerciseSessionViewModel,
                    healthDataCenter =  healthDataCenter
                )
            )

            val smsPermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                        Log.d("SmsPermission", "SEND_SMS permission wurde erteilt. (Dialog)")
                        viewModel.onSmsPermissionResult(isGranted)
                    } else {
                        Log.d("SmsPermission", "SEND_SMS permission wurde verweigert. (Dialog)")
                        viewModel.onSmsPermissionResult(isGranted)
                    // Hier solltest du dem Nutzer erklären, warum die Berechtigung benötigt wird
                        // (z.B. mit einer Snackbar).
                    }
                }
            )

            val permissionsGranted by viewModel.permissionsGranted
            val smsPermissionsGranted by viewModel.smsPermissionGranted.collectAsState()
            val permissions = viewModel.permissions
            var didInit: Boolean
            val onPermissionsResult = {
                viewModel.initialLoad()
                scope.launch {
                    didInit = viewModel.readInitUserInputState()
                    if (!didInit) {
                        viewModel.saveInitUserInputState(true)
                        viewModel.initialUserInput()
                    }
                }
                Unit
            }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()}

            WelcomeScreen(
                healthConnectAvailability = availability,
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)
                                      },
                onLoadData = onPermissionsResult,
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                generateData = {
                    viewModel.generateAllData()
                    sleepSessionViewModel.generateSleepDataWithoutDelete()
                    exerciseSessionViewModel.refreshExerciseSessions()
                    sleepSessionViewModel.refreshSleepSessions()
                },
                deleteAllGeneratedData = {
                    viewModel.deleteAllData()
                    exerciseSessionViewModel.refreshExerciseSessions()
                    sleepSessionViewModel.deleteSleepSession()
                },
                onRequestSmsPermission = {
                    smsPermissionsLauncher.launch(Manifest.permission.SEND_SMS)
                },
                smsPermissionsGranted = smsPermissionsGranted
            )

        }
        composable(
            route = Screen.PrivacyPolicy.route,
            deepLinks = listOf(
                navDeepLink {
                    action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
                }
            )
        ) {
            PrivacyPolicyScreen()
        }
        composable(Screen.SettingsScreen.route){
            SettingsScreen { scope.launch { healthConnectManager.revokeAllPermissions() } }
        }
        composable(Screen.ExerciseSessions.route) {

            val permissionsGranted by exerciseSessionViewModel.permissionsGranted
            val sessionsList by exerciseSessionViewModel.sessionsList
            val permissions = exerciseSessionViewModel.permissions
            val backgroundReadAvailable by exerciseSessionViewModel.backgroundReadAvailable
            val backgroundReadGranted by exerciseSessionViewModel.backgroundReadGranted
            val onPermissionsResult = {exerciseSessionViewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(exerciseSessionViewModel.permissionsLauncher) {
                onPermissionsResult()}
            ExerciseSessionScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                backgroundReadAvailable = backgroundReadAvailable,
                backgroundReadGranted = backgroundReadGranted,
                sessionsList = sessionsList,
                uiState = exerciseSessionViewModel.uiState,
                onInsertClick = {
                    exerciseSessionViewModel.insertExerciseSession()
                },
                onDetailsClick = { uid ->
                    navController.navigate(Screen.ExerciseSessionDetail.route + "/" + uid)
                },
                onDeleteClick = { uid ->
                    exerciseSessionViewModel.deleteExerciseSession(uid)
                },
                onLoadData = onPermissionsResult,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    exerciseSessionViewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.ExerciseSessionDetail.route + "/{$UID_NAV_ARGUMENT}") {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
            val exerciseSessionDetailviewModel: ExerciseSessionDetailViewModel = viewModel(
                factory = ExerciseSessionDetailViewModelFactory(
                    uid = uid,
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by exerciseSessionDetailviewModel.permissionsGranted
            val sessionMetrics by exerciseSessionDetailviewModel.sessionMetrics
            val permissions = exerciseSessionDetailviewModel.permissions
            val onPermissionsResult = {exerciseSessionDetailviewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(exerciseSessionDetailviewModel.permissionsLauncher) {
                onPermissionsResult()}
            ExerciseSessionDetailScreen(
                permissions = permissions,
                permissionsGranted = permissionsGranted,
                sessionMetrics = sessionMetrics,
                uiState = exerciseSessionDetailviewModel.uiState,
                onDetailsClick = { recordType, recordId, seriesRecordsType ->
                    navController.navigate(Screen.RecordListScreen.route + "/" + recordType + "/"+ recordId + "/" + seriesRecordsType)
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    exerciseSessionDetailviewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.RecordListScreen.route + "/{$RECORD_TYPE}" + "/{$UID_NAV_ARGUMENT}" + "/{$SERIES_RECORDS_TYPE}") {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
            val recordTypeString = it.arguments?.getString(RECORD_TYPE)!!
            val seriesRecordsTypeString = it.arguments?.getString(SERIES_RECORDS_TYPE)!!
            val viewModel: RecordListScreenViewModel = viewModel(
                factory = RecordListViewModelFactory(
                    uid = uid,
                    recordTypeString = recordTypeString,
                    seriesRecordsTypeString = seriesRecordsTypeString,
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val recordList = viewModel.recordList
            val permissions = viewModel.permissions
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
            }
            RecordListScreen(
                uid = uid,
                permissions = permissions,
                permissionsGranted = permissionsGranted,
                recordType = RecordType.valueOf(recordTypeString),
                seriesRecordsType = SeriesRecordsType.valueOf(seriesRecordsTypeString),
                recordList = recordList,
                uiState = viewModel.uiState,
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.SleepSessions.route) {
            val permissionsGranted by sleepSessionViewModel.permissionsGranted
            val sessionsList by sleepSessionViewModel.sessionsList
            val permissions = sleepSessionViewModel.permissions
            val onPermissionsResult = {sleepSessionViewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(sleepSessionViewModel.permissionsLauncher) {
                onPermissionsResult()}
            SleepSessionScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                sessionsList = sessionsList,
                uiState = sleepSessionViewModel.uiState,
                onInsertClick = {
                    sleepSessionViewModel.generateSleepData()
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    sleepSessionViewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.DifferentialChanges.route) {
            val viewModel: DifferentialChangesViewModel = viewModel(
                factory = DifferentialChangesViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val changesToken by viewModel.changesToken
            val permissionsGranted by viewModel.permissionsGranted
            val permissions = viewModel.permissions
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                onPermissionsResult()}
            DifferentialChangesScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                changesEnabled = changesToken != null,
                onChangesEnable = { enabled ->
                    viewModel.enableOrDisableChanges(enabled)
                },
                changes = viewModel.changes,
                changesToken = changesToken,
                onGetChanges = {
                    viewModel.getChanges()
                },
                uiState = viewModel.uiState,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                }
            ) { values ->
                permissionsLauncher.launch(values)
            }
        }
        composable(Screen.AppointmentInfo.route) {
            val viewModel: AppointmentInfoViewModel = viewModel(
                factory = AppointmentInfoViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val saveState by viewModel.saveState.collectAsState()

            LaunchedEffect(saveState) {
                if (saveState is AppointmentInfoViewModel.SaveState.Success) {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Terminangabe erfolgreich gespeichert!"
                        )
                    }
                    // 3. Setze den Zustand im ViewModel zurück, um die Snackbar nicht erneut zu zeigen
                    viewModel.resetSaveState()
                }
            }
            AppointmentInfoScreen (
                onAffirmativeClick = {
                    viewModel.saveAppointmentInfo(true)
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onDenyClick = {
                    viewModel.saveAppointmentInfo(false)
                }
            )
        }
        composable(Screen.PhoneAFriend.route) {
            val viewModel: PhoneAFriendViewModel = viewModel(
                factory = PhoneAFriendViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val saveState by viewModel.saveState.collectAsState()
            val smsPermissionsGranted by viewModel.smsPermissionsGranted.collectAsState()

            val smsPermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                        Log.d("SmsPermission", "SEND_SMS permission wurde erteilt. (Screen)")
                        healthConnectManager.updateSmsPermissionStatus()
                    } else {
                        Log.d("SmsPermission", "SEND_SMS permission wurde verweigert. (Screen)")
                        healthConnectManager.updateSmsPermissionStatus()
                        // Hier solltest du dem Nutzer erklären, warum die Berechtigung benötigt wird
                        // (z.B. mit einer Snackbar).
                    }
                }
            )
            LaunchedEffect(saveState) {
                if (saveState is PhoneAFriendViewModel.SaveState.Success) {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Telefonnummer erfolgreich gespeichert!"
                        )
                    }
                    // 3. Setze den Zustand im ViewModel zurück, um die Snackbar nicht erneut zu zeigen
                    viewModel.resetSaveState()
                }
            }
            PhoneAFriendScreen (
                smsPermissionsGranted = smsPermissionsGranted,
                onConfirm = { number ->
                    viewModel.savePhoneNumber(number)
                },
                viewModel = viewModel,
                onRequestSmsPermission = {
                    smsPermissionsLauncher.launch(Manifest.permission.SEND_SMS)
                }
            )
        }
        composable(Screen.GAD7Formular.route) {
            val viewModel: GAD7ViewModel = viewModel(
                factory = GAD7ViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val saveState by viewModel.saveState.collectAsState()

            LaunchedEffect(saveState) {
                if (saveState is GAD7ViewModel.SaveState.Success) {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Formularzustand erfolgreich gespeichert!"
                        )
                    }
                    // 3. Setze den Zustand im ViewModel zurück, um die Snackbar nicht erneut zu zeigen
                    viewModel.resetSaveState()
                }
            }
            GAD7Screen (
                onConfirm = { score ->
                    viewModel.saveGAD7Info(score)
                },
                onDismiss = { }
            )
        }
        composable(Screen.ExerciseChoice.route) {
            val viewModel: ExerciseChoiceViewModel = viewModel(
                factory = ExerciseChoiceViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val saveState by viewModel.saveState.collectAsState()

            LaunchedEffect(saveState) {
                if (saveState is ExerciseChoiceViewModel.SaveState.Success) {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Übungsauswahl erfolgreich gespeichert!"
                        )
                    }
                    // 3. Setze den Zustand im ViewModel zurück, um die Snackbar nicht erneut zu zeigen
                    viewModel.resetSaveState()
                }
            }
            ExerciseChoiceScreen (
                onChoiceSaved = { choice ->
                    viewModel.saveExerciseChoice(choice)
                }
            )
        }
    }
}
