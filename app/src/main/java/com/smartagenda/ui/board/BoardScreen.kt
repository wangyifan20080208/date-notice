package com.smartagenda.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartagenda.AppContainer
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.ingest.ImportJob
import com.smartagenda.ingest.NotificationAccess
import com.smartagenda.ui.board.components.EmptyBoard
import com.smartagenda.ui.board.components.ImportStatusBar
import com.smartagenda.ui.board.components.SectionHeader
import com.smartagenda.ui.board.components.SourceFilterRow
import com.smartagenda.ui.board.components.TaskCard
import com.smartagenda.ui.common.BannerTone
import com.smartagenda.ui.common.DateTimePickerDialog
import com.smartagenda.ui.common.ImportHistoryDialog
import com.smartagenda.ui.common.InlineBanner
import com.smartagenda.ui.common.ManualImportDialog
import com.smartagenda.ui.detail.TaskDetailScreen
import com.smartagenda.ui.detail.TaskDetailViewModel
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun BoardRoute(
    container: AppContainer,
    onOpenTask: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: BoardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                BoardViewModel(
                    repository = container.taskRepository,
                    settingsStore = container.settingsStore,
                    importJobStore = container.importJobStore,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val importJobs by viewModel.importJobs.collectAsStateWithLifecycle()
    var showImportHistory by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var notificationGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var postNotificationsGranted by remember {
        mutableStateOf(NotificationAccess.hasPostPermission(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = NotificationAccess.isGranted(context)
                postNotificationsGranted = NotificationAccess.hasPostPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var missingTimeTask by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }

    val onTaskClick: (TaskEntity) -> Unit = { task ->
        if (task.isTimeMissing || task.datetime == null) {
            missingTimeTask = task
        } else {
            onOpenTask(task.id)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp

        if (expanded) {
            Row(Modifier.fillMaxSize()) {
                BoardContent(
                    state = state,
                    notificationGranted = notificationGranted,
                    postNotificationsGranted = postNotificationsGranted,
                    onSelectSource = viewModel::selectSource,
                    onSelectTab = viewModel::selectTab,
                    onOpenImport = viewModel::openImport,
                    importJobs = importJobs,
                    onOpenImportHistory = { showImportHistory = true },
                    onTaskClick = { task ->
                        if (task.isTimeMissing || task.datetime == null) {
                            missingTimeTask = task
                        } else {
                            selectedTaskId = task.id
                        }
                    },
                    onComplete = viewModel::completeTask,
                    onReopen = viewModel::reopenTask,
                    onDelete = viewModel::deleteTask,
                    onRestore = viewModel::restoreTask,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.weight(1f),
                )

                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(LocalAgendaColors.current.hairline),
                )

                val paneId = selectedTaskId
                if (paneId != null) {
                    val paneViewModel: TaskDetailViewModel = viewModel(
                        key = "pane-$paneId",
                        factory = viewModelFactory {
                            initializer {
                                TaskDetailViewModel(container.taskRepository, paneId)
                            }
                        },
                    )
                    TaskDetailScreen(
                        viewModel = paneViewModel,
                        onBack = { selectedTaskId = null },
                        modifier = Modifier
                            .widthIn(min = 360.dp, max = 420.dp)
                            .fillMaxHeight(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 360.dp, max = 420.dp)
                            .fillMaxHeight()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "选择一条日程查看详情",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalAgendaColors.current.textTertiary,
                        )
                    }
                }
            }
        } else {
            BoardContent(
                state = state,
                notificationGranted = notificationGranted,
                postNotificationsGranted = postNotificationsGranted,
                onSelectSource = viewModel::selectSource,
                onSelectTab = viewModel::selectTab,
                onOpenImport = viewModel::openImport,
                importJobs = importJobs,
                onOpenImportHistory = { showImportHistory = true },
                onTaskClick = onTaskClick,
                onComplete = viewModel::completeTask,
                onReopen = viewModel::reopenTask,
                onDelete = viewModel::deleteTask,
                onRestore = viewModel::restoreTask,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    val taskForDialog = missingTimeTask
    if (taskForDialog != null) {
        DateTimePickerDialog(
            initialMillis = taskForDialog.datetime,
            onDismiss = { missingTimeTask = null },
            onConfirm = { millis ->
                viewModel.setTaskTime(taskForDialog.id, millis)
                missingTimeTask = null
            },
        )
    }

    if (importState.visible) {
        ManualImportDialog(
            state = importState,
            sources = state.sources,
            onSourceChange = viewModel::updateImportSource,
            onContentChange = viewModel::updateImportContent,
            onDismiss = viewModel::dismissImport,
            onSubmit = viewModel::submitImport,
        )
    }

    if (showImportHistory) {
        ImportHistoryDialog(
            jobs = importJobs,
            onDismiss = { showImportHistory = false },
            onClearFinished = {
                viewModel.clearImportHistory()
                showImportHistory = false
            },
        )
    }
}

@Composable
private fun BoardContent(
    state: BoardUiState,
    notificationGranted: Boolean,
    postNotificationsGranted: Boolean,
    onSelectSource: (String?) -> Unit,
    onSelectTab: (BoardTab) -> Unit,
    onOpenImport: () -> Unit,
    importJobs: List<ImportJob>,
    onOpenImportHistory: () -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onComplete: (Long) -> Unit,
    onReopen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAgendaColors.current
    val now = remember { System.currentTimeMillis() }
    val notificationsReady = notificationGranted && postNotificationsGranted

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "日程",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(TimeFormatter.todayHeader())
                        if (state.totalCount > 0) {
                            append(" · ").append(state.totalCount).append(" 项")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )
            }
            TextButton(
                onClick = onOpenImport,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "导入",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (importJobs.isNotEmpty()) {
            ImportStatusBar(
                jobs = importJobs,
                onOpenHistory = onOpenImportHistory,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        BoardTabRow(
            selected = state.tab,
            pendingCount = state.pendingCount,
            doneCount = state.doneCount,
            deletedCount = state.deletedCount,
            onSelect = onSelectTab,
        )
        Spacer(Modifier.height(12.dp))

        if (state.sources.isNotEmpty()) {
            SourceFilterRow(
                sources = state.sources,
                selected = state.selectedSource,
                onSelect = onSelectSource,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (!notificationsReady || !state.aiConfigured) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!notificationsReady) {
                    InlineBanner(
                        title = if (!notificationGranted) "通知读取未开启" else "提醒通知未授权",
                        description = if (!notificationGranted) {
                            "开启通知使用权后，才能自动识别群消息；同时请允许本应用发出提醒。"
                        } else {
                            "到点提醒需要通知权限，请在系统设置中允许。"
                        },
                        actionLabel = "去设置",
                        onAction = onOpenSettings,
                        tone = BannerTone.Warning,
                    )
                }
                if (!state.aiConfigured) {
                    InlineBanner(
                        title = "还没有配置大模型",
                        description = "填入 API Key 后，消息才会被解析成日程。",
                        actionLabel = "去设置",
                        onAction = onOpenSettings,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (state.isEmpty) {
            EmptyBoard(
                title = when (state.tab) {
                    BoardTab.PENDING -> "还没有日程"
                    BoardTab.DONE -> "还没有已完成的日程"
                    BoardTab.DELETED -> "回收站是空的"
                },
                description = when (state.tab) {
                    BoardTab.PENDING -> "群里的会议、作业和集合通知，会被自动提取到这里。"
                    BoardTab.DONE -> "勾选待办前的复选框，完成的任务会出现在这里。"
                    BoardTab.DELETED -> "向右滑动待办卡片即可移到这里，之后可以恢复。"
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.groups.forEachIndexed { index, group ->
                    item(key = "header-${group.key}") {
                        SectionHeader(
                            title = group.title,
                            count = group.tasks.size,
                            hint = group.hint,
                            first = index == 0,
                        )
                    }
                    items(items = group.tasks, key = { it.id }) { task ->
                        if (state.tab == BoardTab.DELETED) {
                            TaskCard(
                                task = task,
                                now = now,
                                mode = BoardTab.DELETED,
                                onClick = { onTaskClick(task) },
                                onToggleDone = {},
                                onRestore = { onRestore(task.id) },
                            )
                        } else {
                            SwipeableTaskCard(
                                task = task,
                                now = now,
                                mode = state.tab,
                                onClick = { onTaskClick(task) },
                                onToggleDone = { checked ->
                                    if (checked) onComplete(task.id) else onReopen(task.id)
                                },
                                onDelete = { onDelete(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardTabRow(
    selected: BoardTab,
    pendingCount: Int,
    doneCount: Int,
    deletedCount: Int,
    onSelect: (BoardTab) -> Unit,
) {
    val extras = LocalAgendaColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        BoardTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val count = when (tab) {
                BoardTab.PENDING -> pendingCount
                BoardTab.DONE -> doneCount
                BoardTab.DELETED -> deletedCount
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(tab) }
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (count > 0) "${tab.label} $count" else tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        extras.textSecondary
                    },
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.55f)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task: TaskEntity,
    now: Long,
    mode: BoardTab,
    onClick: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        TaskCard(
            task = task,
            now = now,
            mode = mode,
            onClick = onClick,
            onToggleDone = onToggleDone,
            onRestore = {},
        )
    }
}