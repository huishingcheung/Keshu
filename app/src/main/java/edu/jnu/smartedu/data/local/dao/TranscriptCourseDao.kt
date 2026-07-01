package edu.jnu.smartedu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jnu.smartedu.data.local.entity.TranscriptCourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptCourseDao {
    @Query("SELECT * FROM transcript_courses ORDER BY name")
    fun observeAll(): Flow<List<TranscriptCourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(courses: List<TranscriptCourseEntity>)
}
