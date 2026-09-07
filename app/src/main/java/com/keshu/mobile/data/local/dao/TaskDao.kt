package com.keshu.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keshu.mobile.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY dueAtMillis")
    fun observeOpenTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY done, dueAtMillis")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY dueAtMillis LIMIT :limit")
    suspend fun getOpenTasks(limit: Int = 3): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE done = 0")
    suspend fun getOpenTaskCount(): Int

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
