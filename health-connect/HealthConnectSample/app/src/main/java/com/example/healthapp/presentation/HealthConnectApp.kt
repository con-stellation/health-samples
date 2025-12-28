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
package com.example.healthapp.presentation

import android.annotation.SuppressLint
import android.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.healthapp.R
import com.example.healthapp.data.HealthConnectManager
import com.example.healthapp.presentation.navigation.Drawer
import com.example.healthapp.presentation.navigation.HealthConnectNavigation
import com.example.healthapp.presentation.navigation.Screen
import com.example.healthapp.presentation.theme.HealthConnectTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val TAG = "Health Connect sample"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HealthConnectApp(healthConnectManager: HealthConnectManager) {
    HealthConnectTheme {
        val scaffoldState = rememberScaffoldState()
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val availability by healthConnectManager.availability

        var startTutorial by remember { mutableStateOf(false) }

        if(startTutorial) {
            TutorialDialog(
                onClick = { startTutorial = true },
                onDismiss = { startTutorial = false }
            )
        }
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = {
                        val titleId = when (currentRoute) {
                            Screen.ExerciseSessions.route -> Screen.ExerciseSessions.titleId
                            Screen.SleepSessions.route -> Screen.SleepSessions.titleId
                            //Screen.DifferentialChanges.route -> Screen.DifferentialChanges.titleId
                            Screen.GAD7Formular.route -> Screen.GAD7Formular.titleId
                            Screen.ExerciseChoice.route -> Screen.ExerciseChoice.titleId
                            Screen.PhoneAFriend.route -> Screen.PhoneAFriend.titleId
                            Screen.AppointmentInfo.route -> Screen.AppointmentInfo.titleId
                            else -> R.string.app_name
                        }
                        Text(stringResource(titleId))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (availability == SDK_AVAILABLE) {
                                    scope.launch {
                                        scaffoldState.drawerState.open()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                stringResource(id = R.string.menu)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                startTutorial = true
                            },
                            enabled = true,
                            content = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Help,
                                    contentDescription = "Hilfe zur Nutzung der App",
                                    tint = Color.White
                                )
                            }
                        )
                        IconButton(
                            onClick = {
                                startTutorial = true
                            },
                            enabled = true,
                            content = {
                                IconButton(
                                    onClick = { },
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.audio_description), tint = Color.White)
                                }
                            }
                        )
                    }
                )
            },
            drawerContent = {
                if (availability == SDK_AVAILABLE) {
                    Drawer(
                        scope = scope,
                        scaffoldState = scaffoldState,
                        navController = navController
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(it) { data -> Snackbar(snackbarData = data) }
            }
        ) {
            HealthConnectNavigation(
                healthConnectManager = healthConnectManager,
                navController = navController,
                scaffoldState = scaffoldState
            )
        }
    }
}

@Composable
fun TutorialDialog(
    onDismiss: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Tutorial")
        },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Willkommen beim Mental Health Assistant Tutorial! Möchten Sie fortfahren? (Mock-Dialog)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onClick() },
                enabled = false
            ) {
                Text(text = stringResource(id = R.string.affirmative))
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() }
            ) {
                Text(text = stringResource(id = R.string.deny))
            }
        }
    )
}
