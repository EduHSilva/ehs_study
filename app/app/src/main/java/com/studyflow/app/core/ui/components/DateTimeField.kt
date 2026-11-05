package com.studyflow.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.domain.tasks.TaskFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Campo de data+hora com os mesmos dois passos do `datetime-local` do web:
 * escolhe o dia no calendário e depois a hora.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    value: LocalDateTime?,
    onChange: (LocalDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true,
) {
    val s = LocalStrings.current
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    if (showDate) {
        val initial = value?.toLocalDate() ?: LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    pendingDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    showDate = false
                    showTime = true
                }) { Text(s["confirmAction"]) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text(s["cancelAction"]) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        val timeState = rememberTimePickerState(
            initialHour = value?.hour ?: 9,
            initialMinute = value?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text(s["timeLabel"]) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = pendingDate ?: LocalDate.now()
                    onChange(date.atTime(timeState.hour, timeState.minute))
                    showTime = false
                    pendingDate = null
                }) { Text(s["confirmAction"]) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text(s["cancelAction"]) }
            },
        )
    }

    OutlinedTextField(
        value = value?.let { TaskFormat.formatDateTime(it, s.locale) }.orEmpty(),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(s["selectDate"]) },
        trailingIcon = {
            Row {
                if (allowClear && value != null) {
                    IconButton(onClick = { onChange(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = s["clearDate"], modifier = Modifier.padding(4.dp))
                    }
                }
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = s["selectDate"],
                    modifier = Modifier
                        .clickable { showDate = true }
                        .padding(12.dp),
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDate = true },
    )
}
