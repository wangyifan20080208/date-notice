package com.smartagenda.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.data.local.TaskStatus
import com.smartagenda.ui.common.DateTimePickerDialog
import com.smartagenda.ui.theme.LocalAgendaColors
import kotlinx.coroutines.delay

@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val task by viewModel.task.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }

    val current = task
    if (current == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "日程不存在或已被删除",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAgendaColors.current.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("返回") }
        }
        return
    }

    TaskDetailContent(
        task = current,
        showBack = true,
        onBack = onBack,
        onComplete = viewModel::complete,
        onReopen = viewModel::reopen,
        onChangeTime = { showPicker = true },
        onMarkDeleted = { viewModel.markDeleted(onBack) },
        onRestore = viewModel::restore,
        onPurge = { viewModel.purge(onBack) },
        onEdit = if (current.status == TaskStatus.DELETED) null else { { showEdit = true } },
        modifier = modifier,
    )

    if (showPicker) {
        DateTimePickerDialog(
            initialMillis = current.datetime,
            title = if (current.datetime == null) "补充提醒时间" else "修改提醒时间",
            onDismiss = { showPicker = false },
            onConfirm = {
                viewModel.setTime(it)
                showPicker = false
            },
        )
    }

    if (showEdit) {
        TaskEditDialog(
            task = current,
            sources = sources,
            onDismiss = { showEdit = false },
            onSave = { title, datetime, location, source, note ->
                viewModel.updateTask(title, datetime, location, source, note)
                showEdit = false
            },
        )
    }
}

@Composable
fun TaskDetailContent(
    task: TaskEntity,
    showBack: Boolean,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onChangeTime: () -> Unit,
    onMarkDeleted: () -> Unit,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAgendaColors.current
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmPurge by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }

    val done = task.status == TaskStatus.DONE
    val deleted = task.status == TaskStatus.DELETED
    val finished = done || deleted

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.weight(1f))
            if (onEdit != null) {
                TextButton(onClick = onEdit) {
                    Text(
                        text = "编辑",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 24.dp)) {
            if (task.isTimeMissing && !finished) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = extras.amberSoft,
                    border = BorderStroke(1.dp, extras.amberLine),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "这条日程缺少具体时间",
                            style = MaterialTheme.typography.labelLarge,
                            color = extras.amber,
                        )
                        Text(
                            text = "补充时间后才会注册提醒。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extras.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (deleted) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = extras.elevated,
                    border = BorderStroke(1.dp, extras.hairline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "这条日程在回收站",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "可以恢复到未完成，也可以彻底删除。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extras.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (finished) TextDecoration.LineThrough else null,
            )

            Spacer(Modifier.height(18.dp))

            if (task.datetime != null) {
                Text(
                    text = TimeFormatter.formatFull(task.datetime),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (finished) extras.textTertiary else MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "时间待定",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (finished) extras.textTertiary else extras.amber,
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = extras.hairline)
            Spacer(Modifier.height(16.dp))

            val appLabel = sourceAppLabel(task.sourceApp)
            MetaRow(
                label = "来源",
                value = if (appLabel == task.source) task.source else "${task.source} · $appLabel",
            )
            if (!task.location.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                MetaRow(label = "地点", value = task.location.orEmpty())
            }
            if (!task.note.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                MetaRow(label = "原文", value = task.note.orEmpty())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(task.note.orEmpty()))
                            copied = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (copied) "已复制" else "复制原文",
                            style = MaterialTheme.typography.labelMedium,
                            color = extras.textTertiary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            MetaRow(
                label = "提醒",
                value = when {
                    task.datetime != null && !finished ->
                        "将在 ${TimeFormatter.formatFull(task.datetime)} 提醒"
                    task.datetime != null -> "提醒已取消"
                    else -> "未设置提醒"
                },
            )
            Spacer(Modifier.height(10.dp))
            MetaRow(
                label = "状态",
                value = when {
                    deleted -> "已删除"
                    done -> "已完成"
                    else -> "待办"
                },
            )

            Spacer(Modifier.height(32.dp))

            when {
                deleted -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRestore,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("恢复")
                        }
                        OutlinedButton(
                            onClick = { confirmPurge = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "彻底删除",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                done -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onReopen,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("恢复为待办")
                        }
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("移到已删除")
                        }
                    }
                }

                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("标记完成")
                        }
                        OutlinedButton(
                            onClick = onChangeTime,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (task.datetime == null) "补充时间" else "修改时间")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(
                            text = "移到已删除",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("移到已删除") },
            text = { Text("之后可以在“删除”分组里恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onMarkDeleted()
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }

    if (confirmPurge) {
        AlertDialog(
            onDismissRequest = { confirmPurge = false },
            title = { Text("彻底删除") },
            text = { Text("彻底删除后无法恢复，对应的提醒也会取消。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmPurge = false
                        onPurge()
                    },
                ) {
                    Text("彻底删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPurge = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val extras = LocalAgendaColors.current
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = extras.textTertiary,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun sourceAppLabel(raw: String): String = when (raw) {
    "WECHAT" -> "微信"
    "QQ" -> "QQ"
    "MANUAL" -> "手动导入"
    else -> raw
}