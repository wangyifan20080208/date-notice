package com.smartagenda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskStatus { PENDING, DONE, DELETED }

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val datetime: Long? = null,
    val source: String,
    val sourceApp: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val isTimeMissing: Boolean = false,
    val location: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val hasTime: Boolean get() = datetime != null
}