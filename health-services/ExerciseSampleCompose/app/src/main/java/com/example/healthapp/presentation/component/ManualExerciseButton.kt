package com.example.healthapp.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import com.example.healthapp.R

@Composable
fun ManualExerciseButton(onStartClick: () -> Unit) {
    FilledIconButton(
        onClick = onStartClick
    ) {
        Icon(
            imageVector = Icons.Default.HealthAndSafety,
            contentDescription = stringResource(id = R.string.monitor_button_cd)
        )
    }
}

@Preview
@Composable
fun ManualExerciseButtonPreview() {
    ManualExerciseButton { }
}
