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
package com.example.healthapp.presentation.preparing

import android.content.ContentValues.TAG
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.healthapp.R
import com.example.healthapp.data.ServiceState
import com.example.healthapp.presentation.ambient.ambientGray
import com.example.healthapp.presentation.dialogs.ExerciseInProgressAlert
import com.example.healthapp.presentation.theme.ThemePreview
import com.example.healthapp.service.ExerciseServiceState
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun PreparingExerciseRoute(
    onStart: () -> Unit,
    onFinishActivity: () -> Unit,
    onNoExerciseCapabilities: () -> Unit,
    onGoals: () -> Unit,
    onStartMonitoring: () -> Unit
) {
    val viewModel = hiltViewModel<PreparingViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    /** Request permissions prior to launching exercise.**/
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            Log.d(TAG, "All required permissions granted")
        }
    }

    SideEffect {
        val preparingState = uiState
        if (preparingState is PreparingScreenState.Preparing &&
            !preparingState.hasExerciseCapabilities
        ) {
            onNoExerciseCapabilities()
        }
    }

    if (uiState.serviceState is ServiceState.Connected) {
        val requiredPermissions = uiState.requiredPermissions
        LaunchedEffect(requiredPermissions) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    AmbientAware { ambientState ->
        PreparingExerciseScreen(
            onStart = {
                viewModel.startMonitoring()
                onStart()
            },
            uiState = uiState,
            onGoals = { onGoals() },
            ambientState = ambientState,
            onMonitor = {
                viewModel.startMonitoring()
                onStartMonitoring()
            }
        )
    }

    if (uiState.isTrackingInAnotherApp) {
        var dismissed by remember { mutableStateOf(false) }
        ExerciseInProgressAlert(
            onNegative = onFinishActivity,
            onPositive = { dismissed = true },
            showDialog = !dismissed
        )
    }
}

/**
 * Screen that appears while the device is preparing the exercise.
 */
@Composable
fun PreparingExerciseScreen(
    uiState: PreparingScreenState,
    ambientState: AmbientState,
    onStart: () -> Unit = {},
    onMonitor: () -> Unit = {},
    onGoals: () -> Unit = {}
) {
    val location = (uiState as? PreparingScreenState.Preparing)?.locationAvailability

    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.BodyText,
        last = ColumnItemType.Button
    )
    ScreenScaffold(
        scrollState = columnState,
        timeText = {},
        contentPadding = contentPadding,
        modifier = Modifier
            .ambientGray(ambientState)
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding
        ) {
            item {
                Text(
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    text = stringResource(id = R.string.preparing_exercise),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 0.15f * LocalConfiguration.current.screenWidthDp.dp
                        )
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = onMonitor,
                            enabled = uiState is PreparingScreenState.Preparing
                        ) {
                            Icon(
                                Icons.Filled.SelfImprovement,
                                contentDescription = stringResource(id = R.string.breathe)
                            )
                        }

                        FilledIconButton(
                            onClick = onStart,
                            enabled = uiState is PreparingScreenState.Preparing
                        ) {
                            Icon(
                                Icons.Default.MonitorHeart,
                                contentDescription = stringResource(id = R.string.monitor)
                            )
                        }
                    }

                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun PreparingExerciseScreenPreview() {
    ThemePreview {
        PreparingExerciseScreen(
            uiState = PreparingScreenState.Preparing(
                serviceState = ServiceState.Connected(
                    ExerciseServiceState()
                ),
                isTrackingInAnotherApp = false,
                requiredPermissions = PreparingViewModel.permissions,
                hasExerciseCapabilities = true
            ),
            ambientState = AmbientState.Interactive
        )
    }
}

@WearPreviewDevices
@Composable
fun PreparingExerciseScreenPreviewAmbient() {
    ThemePreview {
        PreparingExerciseScreen(
            uiState = PreparingScreenState.Preparing(
                serviceState = ServiceState.Connected(
                    ExerciseServiceState()
                ),
                isTrackingInAnotherApp = false,
                requiredPermissions = PreparingViewModel.permissions,
                hasExerciseCapabilities = true
            ),
            ambientState = AmbientState.Ambient()
        )
    }
}
