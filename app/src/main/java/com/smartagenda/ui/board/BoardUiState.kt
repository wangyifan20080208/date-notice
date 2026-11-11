package com.smartagenda.ui.board

import com.smartagenda.data.local.TaskEntity

enum class BoardTab(val label: String) {
    PENDING("未完成"),
    DONE("已完成"),
    DELETED("删除"),
}

data class TaskSectionGroup(
    val key: String,
    val title: String,
    val hint: String?,
    val tasks: List<TaskEntity>,
)

data class BoardUiState(
    val groups: List<TaskSectionGroup> = emptyList(),
    val sources: List<String> = emptyList(),
    val selectedSource: String? = null,
    val query: String = "",
    val tab: BoardTab = BoardTab.PENDING,
    val pendingCount: Int = 0,
    val doneCount: Int = 0,
    val deletedCount: Int = 0,
    val totalCount: Int = 0,
    val aiConfigured: Boolean = false,
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && totalCount == 0
    val hasActiveFilter: Boolean get() = selectedSource != null || query.isNotBlank()
}