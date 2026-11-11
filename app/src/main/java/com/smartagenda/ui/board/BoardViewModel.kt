package com.smartagenda.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartagenda.core.time.TaskSection
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.data.local.TaskStatus
import com.smartagenda.data.prefs.SettingsDataStore
import com.smartagenda.data.repository.TaskRepository
import com.smartagenda.ingest.ImportJob
import com.smartagenda.ingest.ImportJobStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BoardViewModel(
    private val repository: TaskRepository,
    settingsStore: SettingsDataStore,
    private val importJobStore: ImportJobStore,
) : ViewModel() {

    private val selectedSource = MutableStateFlow<String?>(null)
    private val selectedTab = MutableStateFlow(BoardTab.PENDING)
    private val searchQuery = MutableStateFlow("")

    private val filters = combine(selectedSource, searchQuery) { source, query -> source to query }

    private val aiConfigured: StateFlow<Boolean> = settingsStore.settings
        .map { it.isConfigured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val importJobs: StateFlow<List<ImportJob>> = importJobStore.jobs

    val uiState: StateFlow<BoardUiState> = combine(
        repository.allTasks,
        repository.sources,
        filters,
        selectedTab,
        aiConfigured,
    ) { tasks, sources, filter, tab, configured ->
        val now = System.currentTimeMillis()
        val (source, query) = filter
        val effectiveSource = source?.takeIf { it in sources }
        val keyword = query.trim()

        val filtered = tasks
            .filter { effectiveSource == null || it.source == effectiveSource }
            .filter { task ->
                keyword.isEmpty() ||
                    task.title.contains(keyword, ignoreCase = true) ||
                    task.source.contains(keyword, ignoreCase = true) ||
                    task.location?.contains(keyword, ignoreCase = true) == true ||
                    task.note?.contains(keyword, ignoreCase = true) == true
            }

        val current = when (tab) {
            BoardTab.PENDING -> filtered.filter { it.status == TaskStatus.PENDING }
            BoardTab.DONE -> filtered.filter { it.status == TaskStatus.DONE }
                .sortedByDescending { it.updatedAt }
            BoardTab.DELETED -> filtered.filter { it.status == TaskStatus.DELETED }
                .sortedByDescending { it.updatedAt }
        }

        val groups = when (tab) {
            BoardTab.PENDING -> TaskSection.entries.mapNotNull { section ->
                val items = current.filter { TimeFormatter.sectionFor(it.datetime, now) == section }
                if (items.isEmpty()) {
                    null
                } else {
                    TaskSectionGroup(
                        key = section.name,
                        title = section.label,
                        hint = hintFor(section, items),
                        tasks = items,
                    )
                }
            }

            BoardTab.DONE -> doneGroups(current, now)

            BoardTab.DELETED -> if (current.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    TaskSectionGroup(
                        key = "DELETED",
                        title = "已删除",
                        hint = "可恢复到未完成",
                        tasks = current,
                    ),
                )
            }
        }

        BoardUiState(
            groups = groups,
            sources = sources,
            selectedSource = effectiveSource,
            query = query,
            tab = tab,
            pendingCount = tasks.count { it.status == TaskStatus.PENDING },
            doneCount = tasks.count { it.status == TaskStatus.DONE },
            deletedCount = tasks.count { it.status == TaskStatus.DELETED },
            totalCount = current.size,
            aiConfigured = configured,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardUiState())

    fun selectSource(source: String?) {
        selectedSource.value = source
    }

    fun selectTab(tab: BoardTab) {
        selectedTab.value = tab
    }

    fun updateQuery(query: String) {
        searchQuery.value = query
    }

    fun clearQuery() {
        searchQuery.value = ""
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedSource.value = null
    }

    fun createTask(
        title: String,
        datetime: Long?,
        location: String,
        source: String,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        viewModelScope.launch {
            repository.create(
                title = trimmedTitle,
                datetime = datetime,
                source = source.trim().ifBlank { "手动创建" },
                sourceApp = "MANUAL",
                location = location.trim().ifBlank { null },
            )
            selectedTab.value = BoardTab.PENDING
        }
    }

    fun completeTask(id: Long) {
        viewModelScope.launch { repository.complete(id) }
    }

    fun reopenTask(id: Long) {
        viewModelScope.launch { repository.reopen(id) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.markDeleted(id) }
    }

    fun restoreTask(id: Long) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun setTaskTime(id: Long, datetime: Long) {
        viewModelScope.launch { repository.setTime(id, datetime) }
    }

    fun submitImport(source: String, content: String): String? {
        if (content.isBlank()) return "请先粘贴消息内容"
        if (!aiConfigured.value) return "请先在设置里配置大模型"

        importJobStore.enqueue(source, content)
        return null
    }

    fun clearImportHistory() {
        importJobStore.clearFinished()
    }

    private fun doneGroups(tasks: List<TaskEntity>, now: Long): List<TaskSectionGroup> {
        if (tasks.isEmpty()) return emptyList()
        val buckets = tasks.groupBy { task ->
            when (-TimeFormatter.dayOffset(task.updatedAt, now)) {
                in Int.MIN_VALUE..0 -> "今天完成"
                1 -> "昨天完成"
                else -> "更早完成"
            }
        }
        return listOf("今天完成", "昨天完成", "更早完成").mapNotNull { title ->
            buckets[title]?.takeIf { it.isNotEmpty() }?.let { items ->
                TaskSectionGroup(
                    key = title,
                    title = title,
                    hint = null,
                    tasks = items,
                )
            }
        }
    }

    private fun hintFor(section: TaskSection, tasks: List<TaskEntity>): String? = when (section) {
        TaskSection.OVERDUE -> "已超过提醒时间"
        TaskSection.TODAY, TaskSection.TOMORROW ->
            tasks.firstOrNull { it.datetime != null }?.datetime?.let(TimeFormatter::formatDate)

        TaskSection.UPCOMING -> "按时间排序"
        TaskSection.MISSING -> null
    }
}