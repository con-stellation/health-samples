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
package com.example.healthapp.presentation.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.example.healthapp.R
import com.example.healthapp.data.HealthConnectManager

/**
 * Settings screen for managing Health Connect preferences.
 */

@Composable
fun SettingsScreen(
    revokeAllPermissions: () -> Unit,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    var cloudChecked by rememberSaveable { mutableStateOf(false) }
    var tapChecked by rememberSaveable { mutableStateOf(false) }
    var employerChecked by rememberSaveable { mutableStateOf(false) }
    val cloudChoice by viewModel.readCloudPreferences().collectAsState(initial = false)
    val tapChoice by viewModel.readTapPreferences().collectAsState(initial = false)
    val employerChoice by viewModel.readTeambuildingPreferences().collectAsState(initial = false)

    LaunchedEffect(cloudChecked) {
        cloudChecked = cloudChoice
    }

    LaunchedEffect(tapChecked) {
        tapChecked = tapChoice
    }

    LaunchedEffect(employerChecked) {
        employerChecked = employerChoice
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.Left,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                modifier = Modifier.semantics { contentDescription = "Speichereinstellungen" },
                checked = cloudChecked,
                onCheckedChange = { cloudChecked = it; viewModel.saveCloudPreferences(it) }
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = "Gesundheitsdaten in der Cloud sichern?")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.Left,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                modifier = Modifier.semantics { contentDescription = "Freigabe für Ärzte" },
                checked = tapChecked,
                onCheckedChange = { tapChecked = it; viewModel.saveTapPreferences(it) }
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = "Gesundheitsdaten für Ärzte freigeben?")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.Left,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                modifier = Modifier.semantics { contentDescription = "Freigabe für Arbeitgeber" },
                checked = employerChecked,
                onCheckedChange = { employerChecked = it; viewModel.saveTeambuildingPreferences(it) }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Gesundheitsdaten für Arbeitgeber freigeben?")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                val settingsIntent = Intent()
                settingsIntent.action =
                    HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
                context.startActivity(settingsIntent) },
                Modifier.weight(1f)
                ) {
                Text(text = stringResource(id = R.string.manage))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                revokeAllPermissions() },
                Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.disconnect))
            }
        }
    }
}

