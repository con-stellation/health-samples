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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthapp.presentation.theme.HealthConnectTheme
import java.time.ZonedDateTime

/**
 * Shows a week's worth of sleep data.
 */
@Composable
fun PhoneAFriendScreen(
    onConfirm: (String) -> Unit = {},
    viewModel: PhoneAFriendViewModel,
    smsPermissionsGranted: Boolean,
    onRequestSmsPermission: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val existingNumber by viewModel.existingNumber.collectAsState()
    
    LaunchedEffect(existingNumber) { 
        text = existingNumber
    }
    
    fun validateInput(input: String): Boolean {
        return (input.matches(Regex("[\\d\\s()+-]+")) && input.count { it.isDigit() } >= 5) || input == "#"
    }

    Column {
        Text("Bitte geben Sie eine Telefonnummer ein, die im Notfall kontaktiert werden soll. Die Telefonnummer kann nachträglich angepasst werden. \n\nFalls Sie keinen Notfallkontakt haben möchten, geben Sie im Eingabefeld bitte # an und klicken Sie auf 'Bestätigen'.")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                isError = false
            },
            label = { Text(existingNumber) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = isError,
            singleLine = true
        )
        if(isError) {
            Text(
                text = "Bitte geben Sie eine gültige Telefonnummer ein.",
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Button(
                onClick = {
                    if(!smsPermissionsGranted) {
                        onRequestSmsPermission()
                    }
                    if(smsPermissionsGranted) {
                        if (validateInput(text)) {
                            isError = false
                            onConfirm(text) // Nur gültige Nummern weitergeben
                        } else {
                            isError = true
                        }
                    }
                },
            ) {
                Text("Bestätigen")
            }
        }
    }

