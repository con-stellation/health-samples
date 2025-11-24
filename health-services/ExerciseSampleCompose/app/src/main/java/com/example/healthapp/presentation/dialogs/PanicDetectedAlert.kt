package com.example.healthapp.presentation.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.healthapp.R

@Composable
fun PanicDetectedAlert (
    onNegative: () -> Unit,
    onPositive: () -> Unit,
    showDialog: Boolean
) {
    AlertDialog(
        visible = showDialog,
        onDismissRequest = onNegative,
        title = { Text(stringResource(id = R.string.panic_detected))},
        text = {Text(stringResource(id = R.string.panic_detected))},
            confirmButton = {
                Button(
                    onClick = onPositive
                ) {
                    Text(stringResource(id = R.string.yes))
                }
            },
        dismissButton = {
            FilledTonalButton(
                onClick = onNegative
            ) {
                Text(stringResource(id = R.string.no))
            }
        }
    )
}

@WearPreviewDevices
@Composable
fun PanicDetectedAlertPreview() {
    PanicDetectedAlert(onNegative = {}, onPositive = {}, showDialog = true)
}


