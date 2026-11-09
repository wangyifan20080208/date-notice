package com.smartagenda.ui.board.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartagenda.ingest.ImportJob
import com.smartagenda.ingest.ImportJobStatus
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun ImportStatusBar(
    jobs: List<ImportJob>,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (jobs.isEmpty()) return

    val extras = LocalAgendaColors.current
    val active = jobs.count {
        it.status == ImportJobStatus.QUEUED || it.status == ImportJobStatus.PARSING
    }
    val latest = jobs.firstOrNull()
    val failed = latest?.status == ImportJobStatus.FAILED

    val title = when {
        active > 0 -> "正在后台解析 $active 条消息"
        latest?.status == ImportJobStatus.SUCCESS -> if (latest.createdTitles.isNotEmpty()) {
            "解析完成：新增 ${latest.createdTitles.size} 项"
        } else {
            "解析完成：已更新日程"
        }
        failed -> "解析失败：${latest?.error ?: "未知错误"}"
        else -> "解析完成：未识别到日程"
    }

    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onOpenHistory),
        shape = shape,
        color = when {
            active > 0 -> extras.accentSoft
            failed -> extras.amberSoft
            else -> extras.elevated
        },
        border = BorderStroke(1.dp, if (failed) extras.amberLine else extras.hairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (active > 0) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = if (failed) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = when {
                        failed -> extras.amber
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (failed) extras.amber else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "点击查看解析记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.textTertiary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF8B8E92),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}