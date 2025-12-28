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
package com.example.healthapp.presentation.screen.userinputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.healthapp.R
import com.example.healthapp.data.SleepSessionData
import com.example.healthapp.presentation.component.SleepSessionRow
import com.example.healthapp.presentation.screen.sleepsession.SleepSessionViewModel
import com.example.healthapp.presentation.theme.HealthConnectTheme
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Shows a week's worth of sleep data.
 */
@Composable
fun AppointmentInfoScreen(
    //permissions: Set<String>,
    //uiState: AppointmentInfoViewModel.UiState,
    onAffirmativeClick: () -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onDenyClick: () -> Unit = {},
) {

    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text("Haben Sie in den nächsten 7 Tagen ein wichtiges Ereignis oder einen Termin, welcher Sie mental belastet?",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(bottom = 16.dp))

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp),
            onClick = {
                onAffirmativeClick()
            }
        ) {
            Text(stringResource(id = R.string.affirmative))
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp),
            onClick = {
                onDenyClick()
            }
        ) {
            Text(stringResource(id = R.string.deny))
        }
    }
}


@Preview
@Composable
fun AppointmentInfoScreenPreview() {
    HealthConnectTheme {
        val end2 = ZonedDateTime.now()
        val start2 = end2.minusHours(5)
        val end1 = end2.minusDays(1)
        val start1 = end1.minusHours(5)
        AppointmentInfoScreen(
        )
    }
}
