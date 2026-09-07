package com.keshu.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keshu.mobile.data.local.entity.CourseFeedbackStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseFeedbackStatsDao {
    @Query("SELECT * FROM course_feedback_stats ORDER BY courseName, teacher")
    fun observeAll(): Flow<List<CourseFeedbackStatsEntity>>

    @Query("SELECT * FROM course_feedback_stats WHERE courseCode IN (:courseCodes)")
    suspend fun getByCourseCodes(courseCodes: List<String>): List<CourseFeedbackStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stats: List<CourseFeedbackStatsEntity>)
}
