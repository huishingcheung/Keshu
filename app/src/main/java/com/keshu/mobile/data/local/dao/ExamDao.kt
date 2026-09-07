package com.keshu.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keshu.mobile.data.local.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE startsAtMillis >= :now ORDER BY startsAtMillis")
    fun observeUpcoming(now: Long = System.currentTimeMillis()): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY startsAtMillis")
    suspend fun getAll(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE startsAtMillis >= :now ORDER BY startsAtMillis LIMIT :limit")
    suspend fun getUpcoming(now: Long = System.currentTimeMillis(), limit: Int = 3): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exams: List<ExamEntity>)
}
