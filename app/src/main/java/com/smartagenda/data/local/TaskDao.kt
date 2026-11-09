package com.smartagenda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY CASE WHEN datetime IS NULL THEN 1 ELSE 0 END, datetime ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' ORDER BY CASE WHEN datetime IS NULL THEN 1 ELSE 0 END, datetime ASC")
    fun observePending(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'DONE' ORDER BY updatedAt DESC")
    fun observeDone(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'DELETED' ORDER BY updatedAt DESC")
    fun observeDeleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun findById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' AND (datetime IS NULL OR datetime BETWEEN :from AND :to) ORDER BY CASE WHEN datetime IS NULL THEN 1 ELSE 0 END, datetime ASC")
    suspend fun pendingBetween(from: Long, to: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING'")
    suspend fun allPending(): List<TaskEntity>

    @Query("SELECT DISTINCT source FROM tasks WHERE source != '' ORDER BY source ASC")
    fun observeSources(): Flow<List<String>>

    @Query(
        "SELECT * FROM tasks WHERE status != 'DELETED' AND title = :title AND source = :source " +
            "AND ((:datetime IS NULL AND datetime IS NULL) OR datetime = :datetime) LIMIT 1",
    )
    suspend fun findDuplicate(title: String, source: String, datetime: Long?): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus, now: Long)

    @Query("UPDATE tasks SET datetime = :datetime, isTimeMissing = 0, updatedAt = :now WHERE id = :id")
    suspend fun setDatetime(id: Long, datetime: Long, now: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}