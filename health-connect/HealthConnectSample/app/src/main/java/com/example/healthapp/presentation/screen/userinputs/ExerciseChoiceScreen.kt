package com.example.healthapp.presentation.screen.userinputs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Shows a week's worth of sleep data.
 */
@Composable
fun ExerciseChoiceScreen(
    onChoiceSaved: (String) -> Unit = {},
) {

    var selectedIndex by rememberSaveable { mutableStateOf(-1) }
    val exerciseChoices = listOf("4-7-8", "4-7-11", "4-4-4-4")
    val explanations = listOf(
        "4 Sekunden einatmen (starke Vibration), 7 Sekunden halten (keine Vibration), 8 Sekunden ausatmen (schwache Vibration).",
        "4 Sekunden einatmen (starke Vibration), 7 Sekunden halten (keine Vibration), 11 Sekunden ausatmen (schwache Vibration).",
        "4 Sekunden einatmen (starke Vibration), 4 Sekunden halten (keine Vibration), 4 Sekunden ausatmen (schwache Vibration), 4 Sekunden halten (keine Vibration)."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Suchen Sie sich eine Übung aus, die Sie zur Beruhigung verwenden wollen.",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))
        // 2. Scrollbare Liste für alle Fragen
        LazyColumn(
            modifier = Modifier
                .weight(1f) // Nimmt den gesamten verfügbaren Platz ein
                .selectableGroup()
        ) {
            itemsIndexed(exerciseChoices) { index, title ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (selectedIndex == index),
                            onClick = { selectedIndex = index },
                            role = Role.RadioButton
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedIndex == index),
                        onClick = null
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h6
                        )
                        Text(
                            text = explanations[index],
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }

                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onChoiceSaved(exerciseChoices[selectedIndex])
            },
            enabled = selectedIndex != -1,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Bestätigung")
        }
    }
}


