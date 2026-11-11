package com.smartagenda.ui.common

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.smartagenda.ui.theme.LocalAgendaColors

enum class AddMode(val label: String) {
    MANUAL("直接创建"),
    AI("用 AI 解析"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    initialMode: AddMode,
    sources: List<String>,
    aiConfigured: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, datetime: Long?, location: String, source: String) -> Unit,
    onParse: (source: String, content: String) -> String?,
) {
    val extras = LocalAgendaColors.current
    var mode by remember { mutableStateOf(initialMode) }
    var title by remember { mutableStateOf("") }
    var datetime by remember { mutableStateOf<Long?>(null) }
    var location by remember { mutableStateOf("") }
    var source by remember {
        mutableStateOf(if (initialMode == AddMode.AI) "手动导入" else "手动创建")
    }
    var content by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 720.dp)
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
                    text = "添加日程",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "直接填写，或把群消息交给模型整理。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )

                Spacer(Modifier.height(14.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AddMode.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = mode == entry,
                            onClick = {
                                mode = entry
                                error = null
                                if (entry == AddMode.AI && source == "手动创建") {
                                    source = "手动导入"
                                }
                                if (entry == AddMode.MANUAL && source == "手动导入") {
                                    source = "手动创建"
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AddMode.entries.size,
                            ),
                            label = { Text(entry.label) },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (mode == AddMode.MANUAL) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("要做什么") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "提醒时间",
                                style = MaterialTheme.typography.labelMedium,
                                color = extras.textTertiary,
                            )
                            Text(
                                text = datetime?.let(TimeFormatter::formatFull) ?: "暂不设定",
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
                } else {
                    SourcePickerField(
                        value = source,
                        onValueChange = { source = it },
                        sources = sources,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("消息内容") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (aiConfigured) {
                            "提交后转入后台解析，可以继续添加下一条。"
                        } else {
                            "还没有配置大模型，请先到设置里填入 API Key。"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (aiConfigured) extras.textTertiary else extras.amber,
                    )
                }

                val currentError = error
                if (currentError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = currentError,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

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
                            error = null
                            if (mode == AddMode.MANUAL) {
                                if (title.isBlank()) {
                                    error = "请先填写标题"
                                } else {
                                    onCreate(title.trim(), datetime, location.trim(), source.trim())
                                    onDismiss()
                                }
                            } else {
                                if (content.isBlank()) {
                                    error = "请先粘贴消息内容"
                                } else {
                                    val result = onParse(source.trim(), content.trim())
                                    if (result == null) onDismiss() else error = result
                                }
                            }
                        },
                        enabled = if (mode == AddMode.MANUAL) {
                            title.isNotBlank()
                        } else {
                            content.isNotBlank() && aiConfigured
                        },
                    ) {
                        Text(if (mode == AddMode.MANUAL) "创建" else "加入后台解析")
                    }
                }
            }
        }
    }

    if (showPicker) {
        DateTimePickerDialog(
            initialMillis = datetime ?: System.currentTimeMillis(),
            title = "选择提醒时间",
            onDismiss = { showPicker = false },
            onConfirm = {
                datetime = it
                showPicker = false
            },
        )
    }
}