package com.smartagenda.ui.board.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.ui.board.BoardTab
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun TaskCard(
    task: TaskEntity,
    now: Long,
    mode: BoardTab,
    onClick: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    onPickTime: (() -> Unit)? = null,
) {
    val extras = LocalAgendaColors.current
    val highlightMissing = mode == BoardTab.PENDING && (task.isTimeMissing || task.datetime == null)
    val overdue = mode == BoardTab.PENDING && TimeFormatter.isOverdue(task.datetime, now)
    val finished = mode != BoardTab.PENDING
    val shape = MaterialTheme.shapes.medium

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (highlightMissing) extras.amberSoft else MaterialTheme.colorScheme.surface,
        border = if (highlightMissing) {
            BorderStroke(1.dp, extras.amberLine)
        } else {
            BorderStroke(1.dp, extras.hairline)
        },
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            if (highlightMissing) {
                Spacer(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(extras.amber),
                )
            }

            Row(
                modifier = Modifier.padding(
                    start = if (mode == BoardTab.DELETED) 15.dp else 8.dp,
                    end = 12.dp,
                    top = 9.dp,
                    bottom = 9.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (mode != BoardTab.DELETED) {
                    Checkbox(
                        checked = mode == BoardTab.DONE,
                        onCheckedChange = onToggleDone,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = extras.textTertiary,
                        ),
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                }

                TimeBlock(
                    task = task,
                    highlight = highlightMissing,
                    overdue = overdue,
                    onPickTime = onPickTime,
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (finished) extras.textTertiary else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (finished) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.source,
                            style = MaterialTheme.typography.labelMedium,
                            color = extras.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (!task.location.isNullOrBlank()) {
                            Text(
                                text = " · ${task.location}",
                                style = MaterialTheme.typography.labelMedium,
                                color = extras.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        if (overdue) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "已过期",
                                style = MaterialTheme.typography.labelSmall,
                                color = extras.amber,
                            )
                        }
                    }
                }

                if (mode == BoardTab.DONE) {
                    Text(
                        text = TimeFormatter.formatDoneStamp(task.updatedAt, now),
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.textTertiary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                if (mode == BoardTab.DELETED) {
                    TextButton(
                        onClick = onRestore,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Text(
                            text = "恢复",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else if (mode != BoardTab.DONE) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = extras.textTertiary,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeBlock(
    task: TaskEntity,
    highlight: Boolean,
    overdue: Boolean,
    onPickTime: (() -> Unit)?,
) {
    val extras = LocalAgendaColors.current

    if (task.datetime == null) {
        val pickable = if (onPickTime != null) {
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onPickTime)
        } else {
            Modifier
        }
        Column(
            modifier = Modifier
                .width(52.dp)
                .then(pickable)
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "待定",
                style = MaterialTheme.typography.titleMedium,
                color = if (highlight) extras.amber else extras.textTertiary,
            )
            Text(
                text = if (highlight) "点此补充" else "需补充",
                style = MaterialTheme.typography.labelSmall,
                color = if (onPickTime != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    extras.textTertiary
                },
            )
        }
        return
    }

    Column(
        modifier = Modifier.width(52.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = TimeFormatter.formatTime(task.datetime),
            style = MaterialTheme.typography.titleLarge,
            color = when {
                overdue -> extras.amber
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = TimeFormatter.formatCardDate(task.datetime),
            style = MaterialTheme.typography.labelSmall,
            color = extras.textTertiary,
            maxLines = 1,
        )
    }
}