package com.example.gymlog.ui.photo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.PhotoImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Dialogs shared by the photo detail screen and the photo editor: free-text
 * note editing and calendar-date picking.
 */

@Composable
internal fun NoteDialog(
    initial: String,
    title: String = stringResource(R.string.photo_wall_note),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(PhotoImage.NOTE_LIMIT) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoDateDialog(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = epochDayToUtcMillis(initialEpochDay))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = {
                val millis = picker.selectedDateMillis
                if (millis != null) onConfirm(utcMillisToEpochDay(millis))
            }) { Text(stringResource(R.string.photo_wall_confirm_date)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = picker, title = { Text(stringResource(R.string.photo_wall_pick_date)) })
    }
}

/** DatePicker uses UTC midnight. Keep the local calendar date by converting through UTC. */
internal fun epochDayToUtcMillis(epochDay: Long): Long =
    LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun utcMillisToEpochDay(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
