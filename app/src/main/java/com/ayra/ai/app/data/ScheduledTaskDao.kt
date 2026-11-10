package com.ayra.ai.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY scheduledEpochMillis ASC")
    fun getAllTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): ScheduledTask?

    @Query("SELECT * FROM scheduled_tasks WHERE status = 'PENDING' ORDER BY scheduledEpochMillis ASC")
    fun getPendingTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE status = 'PENDING' ORDER BY scheduledEpochMillis ASC")
    suspend fun getPendingTasksSync(): List<ScheduledTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTask): Long

    @Update
    suspend fun updateTask(task: ScheduledTask)

    @Query("UPDATE scheduled_tasks SET status = :newStatus WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, newStatus: String)

    @Query("DELETE FROM scheduled_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("DELETE FROM scheduled_tasks")
    suspend fun clearAllTasks()
}
