package com.smartagenda.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    private val repository: TaskRepository,
    private val taskId: Long,
) : ViewModel() {

    val task: StateFlow<TaskEntity?> = repository.observeTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sources: StateFlow<List<String>> = repository.sources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun complete() {
        viewModelScope.launch { repository.complete(taskId) }
    }

    fun reopen() {
        viewModelScope.launch { repository.reopen(taskId) }
    }

    fun markDeleted(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.markDeleted(taskId)
            onDeleted()
        }
    }

    fun restore() {
        viewModelScope.launch { repository.restore(taskId) }
    }

    fun purge(onPurged: () -> Unit) {
        viewModelScope.launch {
            repository.purge(taskId)
            onPurged()
        }
    }

    fun setTime(datetime: Long) {
        viewModelScope.launch { repository.setTime(taskId, datetime) }
    }

    fun updateTask(
        title: String,
        datetime: Long?,
        location: String?,
        source: String,
        note: String?,
    ) {
        viewModelScope.launch {
            repository.updateTask(
                id = taskId,
                title = title,
                datetime = datetime,
                location = location,
                source = source,
                note = note,
            )
        }
    }
}