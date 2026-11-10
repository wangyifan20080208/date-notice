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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.smartagenda.ui.common.NewTaskDialog
import com.smartagenda.ui.detail.TaskDetailScreen
import com.smartagenda.ui.detail.TaskDetailViewModel
import com.smartagenda.ui.theme.LocalAgendaColors
import kotlinx.coroutines.launch

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
    var showNewTask by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    val onTaskClick: (TaskEntity) -> Unit = { task -> onOpenTask(task.id) }
    val onPickTime: (TaskEntity) -> Unit = { task -> missingTimeTask = task }

    val deleteWithUndo: (Long) -> Unit = { id ->
        viewModel.deleteTask(id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已移到删除",
                actionLabel = "撤销",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTask(id)
            }
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
                    onTaskClick = { task -> selectedTaskId = task.id },
                    onPickTime = onPickTime,
                    onComplete = viewModel::completeTask,
                    onReopen = viewModel::reopenTask,
                    onDelete = deleteWithUndo,
                    onRestore = viewModel::restoreTask,
                    onOpenSettings = onOpenSettings,
                    onNewTask = { showNewTask = true },
                    onClearFilters = viewModel::clearFilters,
                    searchVisible = searchVisible,
                    onToggleSearch = {
                        searchVisible = !searchVisible
                        if (!searchVisible) viewModel.clearQuery()
                    },
                    onQueryChange = viewModel::updateQuery,
                    onClearQuery = viewModel::clearQuery,
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
                onPickTime = onPickTime,
                onComplete = viewModel::completeTask,
                onReopen = viewModel::reopenTask,
                onDelete = deleteWithUndo,
                onRestore = viewModel::restoreTask,
                onOpenSettings = onOpenSettings,
                onNewTask = { showNewTask = true },
                onClearFilters = viewModel::clearFilters,
                searchVisible = searchVisible,
                onToggleSearch = {
                    searchVisible = !searchVisible
                    if (!searchVisible) viewModel.clearQuery()
                },
                onQueryChange = viewModel::updateQuery,
                onClearQuery = viewModel::clearQuery,
                modifier = Modifier.fillMaxSize(),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        )
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

    if (showNewTask) {
        NewTaskDialog(
            sources = state.sources,
            onDismiss = { showNewTask = false },
            onConfirm = { title, datetime, location, source ->
                viewModel.createTask(title, datetime, location, source)
                showNewTask = false
            },
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
    onPickTime: (TaskEntity) -> Unit,
    onComplete: (Long) -> Unit,
    onReopen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onNewTask: () -> Unit,
    onClearFilters: () -> Unit,
    searchVisible: Boolean,
    onToggleSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAgendaColors.current
    val now = remember { System.currentTimeMillis() }
    val notificationsReady = notificationGranted && postNotificationsGranted
    val focusRequester = remember { FocusRequester() }
    var dismissedJobId by remember { mutableStateOf<Long?>(null) }
    val latestJobId = importJobs.firstOrNull()?.id
    val showImportBar = importJobs.isNotEmpty() && latestJobId != dismissedJobId
    val showSourceFilter = state.sources.isNotEmpty() &&
        !(state.isEmpty && !state.hasActiveFilter)

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 20.dp),
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
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = if (searchVisible) "关闭搜索" else "搜索",
                        tint = extras.textSecondary,
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

            if (searchVisible) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text("搜索标题、来源或地点") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = extras.textTertiary,
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "清空搜索",
                                    tint = extras.textTertiary,
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                        .focusRequester(focusRequester),
                )
                LaunchedEffect(searchVisible) {
                    focusRequester.requestFocus()
                }
            }

            if (showImportBar) {
                Spacer(Modifier.height(12.dp))
                ImportStatusBar(
                    jobs = importJobs,
                    onOpenHistory = onOpenImportHistory,
                    onDismiss = { dismissedJobId = latestJobId },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            BoardTabRow(
                selected = state.tab,
                pendingCount = state.pendingCount,
                doneCount = state.doneCount,
                deletedCount = state.deletedCount,
                onSelect = onSelectTab,
            )
            Spacer(Modifier.height(10.dp))

            if (showSourceFilter) {
                SourceFilterRow(
                    sources = state.sources,
                    selected = state.selectedSource,
                    onSelect = onSelectSource,
                )
                Spacer(Modifier.height(10.dp))
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
                when {
                    state.hasActiveFilter -> EmptyBoard(
                        title = "没有匹配的日程",
                        description = "换个关键词，或清除筛选后再看看。",
                        primaryLabel = "清除筛选",
                        onPrimary = onClearFilters,
                        modifier = Modifier.weight(1f),
                    )

                    state.tab == BoardTab.PENDING -> EmptyBoard(
                        title = "还没有日程",
                        description = "群里的会议、作业和集合通知，会被自动提取到这里。",
                        primaryLabel = "导入群消息",
                        onPrimary = onOpenImport,
                        secondaryLabel = "手动新建",
                        onSecondary = onNewTask,
                        modifier = Modifier.weight(1f),
                    )

                    state.tab == BoardTab.DONE -> EmptyBoard(
                        title = "还没有已完成的日程",
                        description = "勾选待办前的复选框，完成的任务会出现在这里。",
                        modifier = Modifier.weight(1f),
                    )

                    else -> EmptyBoard(
                        title = "回收站是空的",
                        description = "向右滑动待办卡片即可移到这里，之后可以恢复。",
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
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
                                    onPickTime = { onPickTime(task) },
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

        FloatingActionButton(
            onClick = onNewTask,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "新建日程",
            )
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
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(tab) }
                    .padding(top = 11.dp, bottom = 9.dp),
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
    onPickTime: () -> Unit,
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
            onPickTime = onPickTime,
        )
    }
}