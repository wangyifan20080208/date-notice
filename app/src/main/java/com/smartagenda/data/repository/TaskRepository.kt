package com.smartagenda.data.repository

import com.smartagenda.data.local.TaskDao
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.data.local.TaskStatus
import com.smartagenda.reminder.ReminderNotifier
import com.smartagenda.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) {

    val pendingTasks: Flow<List<TaskEntity>> = dao.observePending()
    val doneTasks: Flow<List<TaskEntity>> = dao.observeDone()
    val deletedTasks: Flow<List<TaskEntity>> = dao.observeDeleted()
    val allTasks: Flow<List<TaskEntity>> = dao.observeAllTasks()
    val sources: Flow<List<String>> = dao.observeSources()

    fun observeTask(id: Long): Flow<TaskEntity?> = dao.observeById(id)

    suspend fun create(
        title: String,
        datetime: Long?,
        source: String,
        sourceApp: String,
        location: String? = null,
        note: String? = null,
        isTimeMissing: Boolean = datetime == null,
    ): Long {
        val duplicate = dao.findDuplicate(title.trim(), source.trim(), datetime)
        if (duplicate != null) return duplicate.id

        val now = System.currentTimeMillis()
        val entity = TaskEntity(
            title = title.trim(),
            datetime = datetime,
            source = source.trim(),
            sourceApp = sourceApp,
            status = TaskStatus.PENDING,
            isTimeMissing = isTimeMissing || datetime == null,
            location = location?.trim()?.takeIf { it.isNotEmpty() },
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
        )
        val id = dao.insert(entity)
        if (entity.datetime != null) {
            scheduler.schedule(entity.copy(id = id))
        }
        return id
    }

    suspend fun setTime(id: Long, datetime: Long) {
        dao.setDatetime(id, datetime, System.currentTimeMillis())
        dao.findById(id)?.let(scheduler::schedule)
    }

    suspend fun updateFromAi(
        id: Long,
        title: String? = null,
        datetime: Long? = null,
        location: String? = null,
    ) {
        val current = dao.findById(id) ?: return
        val updated = current.copy(
            title = title?.takeIf { it.isNotBlank() } ?: current.title,
            datetime = datetime ?: current.datetime,
            isTimeMissing = if (datetime != null) false else current.isTimeMissing,
            location = location?.takeIf { it.isNotBlank() } ?: current.location,
            updatedAt = System.currentTimeMillis(),
        )
        dao.update(updated)
        if (updated.datetime != null) scheduler.schedule(updated)
    }

    suspend fun updateTask(
        id: Long,
        title: String,
        datetime: Long?,
        location: String?,
        source: String,
        note: String? = null,
    ) {
        val current = dao.findById(id) ?: return
        val updated = current.copy(
            title = title.trim().ifBlank { current.title },
            datetime = datetime,
            isTimeMissing = datetime == null,
            location = location?.trim()?.takeIf { it.isNotEmpty() },
            source = source.trim().ifBlank { current.source },
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = System.currentTimeMillis(),
        )
        dao.update(updated)
        if (updated.status == TaskStatus.PENDING && updated.datetime != null) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(id)
        }
    }

    suspend fun complete(id: Long) {
        dao.updateStatus(id, TaskStatus.DONE, System.currentTimeMillis())
        scheduler.cancel(id)
        notifier.cancel(id)
    }

    suspend fun reopen(id: Long) {
        dao.updateStatus(id, TaskStatus.PENDING, System.currentTimeMillis())
        dao.findById(id)?.let(scheduler::schedule)
    }

    suspend fun markDeleted(id: Long) {
        dao.updateStatus(id, TaskStatus.DELETED, System.currentTimeMillis())
        scheduler.cancel(id)
        notifier.cancel(id)
    }

    suspend fun restore(id: Long) {
        dao.updateStatus(id, TaskStatus.PENDING, System.currentTimeMillis())
        dao.findById(id)?.let(scheduler::schedule)
    }

    suspend fun purge(id: Long) {
        dao.deleteById(id)
        scheduler.cancel(id)
        notifier.cancel(id)
    }

    suspend fun delete(id: Long) {
        purge(id)
    }

    suspend fun pendingWithin(days: Int = 7): List<TaskEntity> {
        val now = System.currentTimeMillis()
        val end = now + days.toLong() * 24L * 60L * 60L * 1_000L
        return dao.pendingBetween(now, end)
    }

    suspend fun allPending(): List<TaskEntity> = dao.allPending()
}