package com.smartagenda.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.ui.theme.LocalAgendaColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    title: String = "补充提醒时间",
) {
    val extras = LocalAgendaColors.current
    val base = initialMillis ?: System.currentTimeMillis()
    val localDateTime = remember(base) { TimeFormatter.toLocalDateTime(base) }

    var step by remember { mutableIntStateOf(0) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = TimeFormatter.toLocalDate(base)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )
    val timeState = rememberTimePickerState(
        initialHour = localDateTime.hour,
        initialMinute = localDateTime.minute,
        is24Hour = true,
    )

    val selectedDate: LocalDate = dateState.selectedDateMillis
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        ?: TimeFormatter.toLocalDate(System.currentTimeMillis())

    val resultMillis = TimeFormatter.toEpochMillis(
        selectedDate,
        timeState.hour,
        timeState.minute,
    )
    val isPast = resultMillis <= System.currentTimeMillis()

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { step = 1 }) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            },
        ) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                )
                DatePicker(
                    state = dateState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                )
            }
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 380.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = TimeFormatter.formatDate(
                        TimeFormatter.toEpochMillis(selectedDate, 0, 0),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )

                Spacer(Modifier.height(14.dp))
                TimePicker(
                    state = timeState,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                if (isPast) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "这个时间已经过去，请重新选择。",
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.amber,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { step = 0 }) { Text("上一步") }
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(
                        onClick = { onConfirm(resultMillis) },
                        enabled = !isPast,
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}