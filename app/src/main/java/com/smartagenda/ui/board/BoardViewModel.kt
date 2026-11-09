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

    private val aiConfigured: StateFlow<Boolean> = settingsStore.settings
        .map { it.isConfigured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _importState = MutableStateFlow(ManualImportUiState())
    val importState: StateFlow<ManualImportUiState> = _importState.asStateFlow()

    val importJobs: StateFlow<List<ImportJob>> = importJobStore.jobs

    val uiState: StateFlow<BoardUiState> = combine(
        repository.allTasks,
        repository.sources,
        selectedSource,
        selectedTab,
        aiConfigured,
    ) { tasks, sources, source, tab, configured ->
        val now = System.currentTimeMillis()
        val effectiveSource = source?.takeIf { it in sources }
        val filtered = if (effectiveSource == null) {
            tasks
        } else {
            tasks.filter { it.source == effectiveSource }
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

            BoardTab.DONE -> if (current.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    TaskSectionGroup(
                        key = "DONE",
                        title = "已完成",
                        hint = "按完成时间",
                        tasks = current,
                    ),
                )
            }

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

    fun openImport() {
        _importState.update { it.copy(visible = true, error = null) }
    }

    fun dismissImport() {
        _importState.update { it.copy(visible = false) }
    }

    fun updateImportSource(value: String) {
        _importState.update { it.copy(source = value, error = null) }
    }

    fun updateImportContent(value: String) {
        _importState.update { it.copy(content = value, error = null) }
    }

    fun submitImport() {
        val state = _importState.value
        if (state.content.isBlank()) {
            _importState.update { it.copy(error = "请先粘贴消息内容") }
            return
        }
        if (!aiConfigured.value) {
            _importState.update { it.copy(error = "请先在设置里配置大模型") }
            return
        }

        importJobStore.enqueue(state.source, state.content)
        _importState.value = ManualImportUiState()
    }

    fun clearImportHistory() {
        importJobStore.clearFinished()
    }

    private fun hintFor(section: TaskSection, tasks: List<TaskEntity>): String? = when (section) {
        TaskSection.TODAY, TaskSection.TOMORROW ->
            tasks.firstOrNull { it.datetime != null }?.datetime?.let(TimeFormatter::formatDate)

        TaskSection.UPCOMING -> "按时间排序"
        TaskSection.MISSING -> null
    }
}