package edu.jnu.smartedu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jnu.smartedu.data.local.entity.ClassSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassScheduleDao {
    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek, startSection, courseName")
    fun observeAll(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE term = :term ORDER BY dayOfWeek, startSection, courseName")
    fun observeByTerm(term: String): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions ORDER BY term DESC, dayOfWeek, startSection, courseName")
    suspend fun getAll(): List<ClassSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<ClassSessionEntity>)

    @Query(
        """
        UPDATE class_sessions
        SET term = :term,
            courseName = :courseName,
            teacher = :teacher,
            weeksText = :weeksText,
            dayOfWeek = :dayOfWeek,
            startSection = :startSection,
            endSection = :endSection,
            location = :location,
            className = :className
        WHERE id = :id
        """,
    )
    suspend fun updateSession(
        id: String,
        term: String,
        courseName: String,
        teacher: String,
        weeksText: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        location: String,
        className: String,
    )
}
