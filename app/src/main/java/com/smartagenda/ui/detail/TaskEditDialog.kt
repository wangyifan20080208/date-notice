package com.smartagenda.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.ui.common.DateTimePickerDialog
import com.smartagenda.ui.common.SourcePickerField
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun TaskEditDialog(
    task: TaskEntity,
    sources: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, datetime: Long?, location: String?, source: String, note: String?) -> Unit,
) {
    val extras = LocalAgendaColors.current
    var title by remember(task.id) { mutableStateOf(task.title) }
    var datetime by remember(task.id) { mutableStateOf(task.datetime) }
    var location by remember(task.id) { mutableStateOf(task.location.orEmpty()) }
    var source by remember(task.id) { mutableStateOf(task.source) }
    var note by remember(task.id) { mutableStateOf(task.note.orEmpty()) }
    var showPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 700.dp)
                .imePadding(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    text = "编辑日程",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "时间",
                            style = MaterialTheme.typography.labelMedium,
                            color = extras.textTertiary,
                        )
                        Text(
                            text = datetime?.let { TimeFormatter.formatFull(it) } ?: "待定",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (datetime == null) {
                                extras.amber
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    if (datetime != null) {
                        TextButton(onClick = { datetime = null }) {
                            Text("清除", color = extras.textTertiary)
                        }
                    }
                    TextButton(onClick = { showPicker = true }) {
                        Text(if (datetime == null) "选择时间" else "修改时间")
                    }
                }

                Spacer(Modifier.height(10.dp))
                SourcePickerField(
                    value = source,
                    onValueChange = { source = it },
                    sources = sources,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注 / 原文（可选）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                title.trim(),
                                datetime,
                                location.trim().ifEmpty { null },
                                source.trim(),
                                note.trim().ifEmpty { null },
                            )
                        },
                        enabled = title.isNotBlank(),
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }

    if (showPicker) {
        DateTimePickerDialog(
            initialMillis = datetime,
            title = if (datetime == null) "选择时间" else "修改时间",
            onDismiss = { showPicker = false },
            onConfirm = {
                datetime = it
                showPicker = false
            },
        )
    }
}