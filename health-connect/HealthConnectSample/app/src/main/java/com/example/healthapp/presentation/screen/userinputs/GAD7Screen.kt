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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthapp.R
import com.example.healthapp.presentation.theme.HealthConnectTheme
import java.time.ZonedDateTime

/**
 * Shows a week's worth of sleep data.
 */

data class Gad7Question(
    val questionText: String,
    val options: List<String> = listOf(
        "Beinahe jeden Tag", // 0 Punkte
        "An mehr als der Hälfte der Tage", // 1 Punkt
        "An einzelnen Tagen", // 2 Punkte
        "Überhaupt nicht" // 3 Punkte
    )
)
private val gad7Questions = listOf(
    Gad7Question("Optional: Im Folgenden werden Ihnen 7 Fragen aus dem GAD7 Fragebogen für die Einordnung Ihrer Angstsymptomatik gestellt.\nDie Fragen beziehen sich jeweils auf die vergangenen 2 Wochen.\nSie können den Fragebogen jederzeit in den Nutzereinstellungen beantworten.\nMöchten Sie fortfahren?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch Nervosität, Ängstlichkeit oder Anspannung beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen beeinträchtigt, weil Sie Ihre Sorgen nicht anhalten oder kontrollieren konnten?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch übermäßige Sorgen bezüglich verschiedener Angelegenheiten beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen beeinträchtigt, da Sie Schwierigkeiten hatten, sich zu entspannen?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch Rastlosigkeit (so dass das Stillsitzen schwerfällt) beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen durch schnelle Veränderung oder Gereiztheit in Ihnen beeinträchtigt?"),
    Gad7Question("Wie oft fühlten Sie sich in den letzten 2 Wochen beeinträchtigt durch ein Angstgefühl, so als würde etwas Schlimmes passieren?")
)

@Composable
fun GAD7Screen(
    onConfirm: (totalScore: Int) -> Unit,
    onDismiss: () -> Unit // Behalten wir für einen "Abbrechen"-Button
) {
    // 1. Zustand für die Antworten: Map<Frage-Index, Antwort-Index>
    // rememberSaveable stellt sicher, dass die Antworten eine Bildschirm-Rotation überleben.
    val answers = rememberSaveable { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    // Die tatsächlichen Fragen, die wir anzeigen (ohne die initiale Ja/Nein-Frage)
    val surveyQuestions = gad7Questions.drop(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Titel des Fragebogens
        Text(
            text = "Fragebogen zur psychischen Gesundheit (GAD-7)",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.gad7_instruction), // Eine neue String-Ressource für die Anleitung
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 2. Scrollbare Liste für alle Fragen
        LazyColumn(
            modifier = Modifier.weight(1f) // Nimmt den gesamten verfügbaren Platz ein
        ) {
            itemsIndexed(surveyQuestions) { index, question ->
                Gad7QuestionItem(
                    question = question,
                    questionIndex = index,
                    selectedAnswer = answers.value[index], // Die aktuell für diese Frage gewählte Antwort
                    onAnswerSelected = { questionIdx, answerIdx ->
                        // Aktualisiere die Map mit der neuen Antwort
                        answers.value = answers.value + (questionIdx to answerIdx)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 3. Buttons am unteren Rand
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }

            Button(
                onClick = {
                    // 4. Berechne den Score und rufe den Callback auf
                    val totalScore = answers.value.values.sum()
                    onConfirm(totalScore)
                },
                // Aktiviere den Button nur, wenn alle Fragen beantwortet sind
                enabled = answers.value.size == surveyQuestions.size
            ) {
                Text("Fertigstellen")
            }
        }
    }
}

/**
 * Eine Composable-Funktion, die eine einzelne GAD-7-Frage darstellt.
 */
@Composable
private fun Gad7QuestionItem(
    question: Gad7Question,
    questionIndex: Int,
    selectedAnswer: Int?,
    onAnswerSelected: (questionIndex: Int, answerIndex: Int) -> Unit
) {
    Column {
        // Fragetext
        Text(
            text = "${questionIndex + 1}. ${question.questionText}",
            style = MaterialTheme.typography.subtitle1
        )
        Spacer(Modifier.height(8.dp))

        // Antwortoptionen
        question.options.forEachIndexed { answerIndex, optionText ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnswerSelected(questionIndex, answerIndex) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedAnswer == answerIndex),
                    onClick = { onAnswerSelected(questionIndex, answerIndex) }
                )
                Text(
                    text = optionText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}


@Preview
@Composable
fun GAD7ScreenPreview() {
    HealthConnectTheme {
        val end2 = ZonedDateTime.now()
        val start2 = end2.minusHours(5)
        val end1 = end2.minusDays(1)
        val start1 = end1.minusHours(5)

    }
}
