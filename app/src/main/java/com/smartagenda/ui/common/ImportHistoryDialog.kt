package com.smartagenda.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.ingest.ImportJob
import com.smartagenda.ingest.ImportJobStatus
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun ImportHistoryDialog(
    jobs: List<ImportJob>,
    onDismiss: () -> Unit,
    onClearFinished: () -> Unit,
) {
    val extras = LocalAgendaColors.current
    val hasFinished = jobs.any {
        it.status == ImportJobStatus.SUCCESS ||
            it.status == ImportJobStatus.IGNORED ||
            it.status == ImportJobStatus.FAILED
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    text = "解析记录",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "提交后会在后台依次解析，这里可以看到每条消息的进度和结果。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )
                Spacer(Modifier.height(12.dp))

                if (jobs.isEmpty()) {
                    Text(
                        text = "还没有解析记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extras.textTertiary,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 460.dp)) {
                        items(items = jobs, key = { it.id }) { job ->
                            ImportJobRow(job)
                            HorizontalDivider(color = extras.hairline)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onClearFinished,
                        enabled = hasFinished,
                    ) {
                        Text("清空已完成")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun ImportJobRow(job: ImportJob) {
    val extras = LocalAgendaColors.current
    val active = job.status == ImportJobStatus.QUEUED || job.status == ImportJobStatus.PARSING
    val failed = job.status == ImportJobStatus.FAILED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (active) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = if (failed) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = when {
                    failed -> extras.amber
                    job.status == ImportJobStatus.IGNORED -> extras.textTertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(17.dp),
            )
        }

        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = job.source,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = TimeFormatter.formatTime(job.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.textTertiary,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = statusLabel(job),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    failed -> extras.amber
                    active -> MaterialTheme.colorScheme.primary
                    else -> extras.textSecondary
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = job.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = extras.textSecondary,
                maxLines = 2,
            )
            val detail = resultDetail(job)
            if (detail != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (failed) extras.amber else extras.textTertiary,
                )
            }
        }
    }
}

private fun statusLabel(job: ImportJob): String = when (job.status) {
    ImportJobStatus.QUEUED -> "排队中"
    ImportJobStatus.PARSING -> "解析中…"
    ImportJobStatus.SUCCESS -> if (job.createdTitles.isNotEmpty()) "已完成" else "已更新"
    ImportJobStatus.IGNORED -> "未识别到日程"
    ImportJobStatus.FAILED -> "解析失败"
}

private fun resultDetail(job: ImportJob): String? = when (job.status) {
    ImportJobStatus.SUCCESS -> if (job.createdTitles.isNotEmpty()) {
        "已添加 ${job.createdTitles.size} 项：" + job.createdTitles.joinToString("、")
    } else {
        "已更新原有日程"
    }

    ImportJobStatus.FAILED -> job.error
    ImportJobStatus.IGNORED -> "可以补充更多内容后重新导入"
    else -> null
}