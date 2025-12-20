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

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.R
import com.example.healthapp.presentation.component.InstalledMessage
import com.example.healthapp.presentation.component.NotInstalledMessage
import com.example.healthapp.presentation.component.NotSupportedMessage
import com.example.healthapp.presentation.theme.HealthConnectTheme

/**
 * Welcome screen shown when the app is first launched.
 */
@Composable
fun WelcomeScreen(
    healthConnectAvailability: Int,
    onPermissionsLaunch: (Set<String>) -> Unit,
    permissionsGranted: Boolean,
    permissions: Set<String>,
    onLoadData: () -> Unit,
    generateData: () -> Unit,
    deleteAllGeneratedData: () -> Unit = {},
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    viewModel: WelcomeScreenViewModel = viewModel()
) {
    val initialInputStep by viewModel.initUserInputStep.collectAsState()
    val currentOnLoadData by rememberUpdatedState(onLoadData)
    // Add a listener to re-check whether Health Connect has been installed each time the Welcome
    // screen is resumed: This ensures that if the user has been redirected to the Play store and
    // followed the onboarding flow, then when the app is resumed, instead of showing the message
    // to ask the user to install Health Connect, the app recognises that Health Connect is now
    // available and shows the appropriate welcome.

    when(initialInputStep) {
        initialUserInputSteps.PhoneNumber -> {
            PhoneNumberDialog(
                onConfirm = { number -> viewModel.onPhoneNumberConfirmed(number) },
                onDismiss = { viewModel.cancelInitUserInput() }
            )
        }
        initialUserInputSteps.AppointmentInfo -> {
            AppointmentDialog(
                onConfirm = { viewModel.onAppointmentConfirmed() },
                onDismiss = { viewModel.cancelInitUserInput() }
            )
        }
        initialUserInputSteps.GAD7 -> {
            GAD7Dialog(
                onConfirm = { score: Int ->
                    viewModel.onGAD7Confirmed(score) },
                onDismiss = { viewModel.cancelInitUserInput() }
            )
        }
        else -> {

        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("WelcomeScreen", "LoadData???")
                currentOnLoadData()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(healthConnectAvailability) {
        Log.d("WelcomeScreen", "LaunchedEffect, permissionsgranted? $permissionsGranted")
        if(healthConnectAvailability == SDK_AVAILABLE){
            if (!permissionsGranted) {
                Log.d("WelcomeScreen", "onPermissionsLaunch")
                onPermissionsLaunch(permissions)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(0.5f),
            painter = painterResource(id = R.drawable.ic_health_connect_logo),
            contentDescription = stringResource(id = R.string.health_connect_logo)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.welcome_message),
            color = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))
        when (healthConnectAvailability) {
            SDK_AVAILABLE -> InstalledMessage()
            SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> NotInstalledMessage()
            SDK_UNAVAILABLE -> NotSupportedMessage()
        }
        Spacer(modifier = Modifier.height(200.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp),
            onClick = {
                generateData()
            }) {
            Text(stringResource(id = R.string.generate_data))
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp),
            onClick = {
                deleteAllGeneratedData()
            }) {
            Text(stringResource(id = R.string.delete_data))
        }
    }

}

@Composable
fun PhoneNumberDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notfallkontakt") },
        text = {
            Column {
                Text("Bitte gib eine Telefonnummer ein, die im Notfall kontaktiert werden soll.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Telefonnummer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() // Aktiviere den Button nur, wenn Text eingegeben wurde
            ) {
                Text("Bestätigen")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

data class Gad7Question(
    val questionText: String,
    val options: List<String> = listOf(
        "Überhaupt nicht", // 0 Punkte
        "An einzelnen Tagen", // 1 Punkt
        "An mehr als der Hälfte der Tage", // 2 Punkte
        "Beinahe jeden Tag" // 3 Punkte
    )
)
private val gad7Questions = listOf(
    Gad7Question("Im Folgenden werden Ihnen 7 Fragen aus dem GAD7 Fragebogen für die Einordnung Ihrer Angstsymptomatik gestellt.\nDie Fragen beziehen sich jeweils auf die vergangenen 2 Wochen.\nDie Beantwortung ist freiwillig. Sie können den Fragebogen jederzeit in den Nutzereinstellungen beantworten.\nMöchten Sie fortfahren?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch Nervosität, Ängstlichkeit oder Anspannung beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen beeinträchtigt, weil Sie Ihre Sorgen nicht anhalten oder kontrollieren konnten?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch übermäßige Sorgen bezüglich verschiedener Angelegenheiten beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen beeinträchtigt, da Sie Schwierigkeiten hatten, sich zu entspannen?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch Rastlosigkeit (so dass das Stillsitzen schwerfällt) beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch schnelle Veränderung oder Gereiztheit in Ihnen beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen beeinträchtigt durch ein Angstgefühl, so als würde etwas Schlimmes passieren?")
)
@Composable
fun GAD7Dialog(
    onDismiss: () -> Unit,
    onConfirm: (totalScore: Int) -> Unit
) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    // Zustand, um die gegebenen Antworten (als Index 0-3) zu speichern
    val answers = remember { mutableStateListOf<Int>() }

    // Die aktuell anzuzeigende Frage
    val currentQuestion = gad7Questions[currentQuestionIndex]

    // Der Zustand für die aktuell ausgewählte Option in der UI
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("GAD-7 Frage ${currentQuestionIndex + 1}/${gad7Questions.size}")
        },
        text = {
            Column {
                Text(currentQuestion.questionText)
                Spacer(Modifier.height(16.dp))
                // Zeige die Antwortoptionen als RadioButtons an
                if(currentQuestionIndex > 0) {
                    currentQuestion.options.forEachIndexed { index, optionText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOptionIndex = index },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedOptionIndex == index),
                                onClick = { selectedOptionIndex = index }
                            )
                            Text(text = optionText, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                // Der "Weiter"-Button
                onClick = {
                    if(currentQuestionIndex == 0) {
                        currentQuestionIndex++
                        selectedOptionIndex = null
                    } else {
                        selectedOptionIndex?.let { answerIndex ->
                            // Speichere die gegebene Antwort
                            answers.add(answerIndex)

                            // Prüfe, ob es die letzte Frage war
                            if (currentQuestionIndex < gad7Questions.size - 1) {
                                // Gehe zur nächsten Frage
                                currentQuestionIndex++
                                // Setze die Auswahl für die neue Frage zurück
                                selectedOptionIndex = null
                            } else {
                                // TEST IST FERTIG
                                // Berechne den Gesamt-Score (der Index ist gleichzeitig der Punktwert)
                                val totalScore = answers.sum()
                                // Rufe den Callback mit dem Ergebnis auf
                                onConfirm(totalScore)
                            }
                        }
                    }

                },
                // Aktiviere den Button nur, wenn eine Antwort ausgewählt wurde
                enabled = (selectedOptionIndex != null) || (currentQuestionIndex == 0)
            ) {
                // Ändere den Text des Buttons auf der letzten Frage
                val buttonText = if (currentQuestionIndex < gad7Questions.size - 1) "Weiter" else "Fertigstellen"
                Text(buttonText)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun AppointmentDialog(
    onConfirm: (totalScore: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Haben Sie in den nächsten 7 Tagen ein wichtiges Ereignis oder einen Termin, welcher Sie mental belastet?")
        },
        text = {
            Column {
                val options = listOf("Nein", "Ja")
                options.forEachIndexed { index, optionText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOptionIndex = index },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOptionIndex == index),
                            onClick = { selectedOptionIndex = index }
                        )
                        Text(text = optionText, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                // Der "Weiter"-Button
                onClick = {
                    selectedOptionIndex?.let { answerIndex ->
                        onConfirm(answerIndex)
                    }
                },
            ) {
                Text("Weiter")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Preview
@Composable
fun InstalledMessagePreview() {
    HealthConnectTheme {
        WelcomeScreen(
            healthConnectAvailability = SDK_AVAILABLE,
            onLoadData = {},
            onPermissionsLaunch = {},
            permissionsGranted = false,
            permissions = setOf(),
            generateData = {}
        )
    }
}

@Preview
@Composable
fun NotInstalledMessagePreview() {
    HealthConnectTheme {
        WelcomeScreen(
            healthConnectAvailability = SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
            onLoadData = {},
            onPermissionsLaunch = {},
            permissionsGranted = false,
            permissions = setOf(),
            generateData = {}
        )

    }
}

@Preview
@Composable
fun NotSupportedMessagePreview() {
    HealthConnectTheme {
        WelcomeScreen(
            healthConnectAvailability = SDK_UNAVAILABLE,
            onLoadData = {},
            onPermissionsLaunch = {},
            permissionsGranted = false,
            permissions = setOf(),
            generateData = {}
        )
    }
}
