package edu.jnu.smartedu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jnu.smartedu.data.local.entity.CourseEntity
import edu.jnu.smartedu.data.local.entity.CourseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY name")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE smallGroupId = :smallGroupId ORDER BY name")
    fun observeBySmallGroup(smallGroupId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE status = :status ORDER BY credit DESC, name")
    suspend fun getByStatus(status: CourseStatus): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CourseEntity?

    @Query("UPDATE courses SET status = :status, score = :score, term = :term WHERE id = :id")
    suspend fun updateTranscriptFields(id: String, status: CourseStatus, score: Double?, term: String?)

    @Query("UPDATE courses SET status = 'TAKING', term = COALESCE(:term, term) WHERE code = :code AND status != 'PASSED'")
    suspend fun markTakingByCode(code: String, term: String?)

    @Query(
        """
        SELECT c.* FROM courses c
        JOIN small_groups sg ON sg.id = c.smallGroupId
        WHERE c.status IN ('NOT_TAKEN', 'FAILED')
          AND sg.id IN (:unmetSmallGroupIds)
        ORDER BY c.credit DESC, c.name
        """,
    )
    suspend fun getAdvisorCandidates(unmetSmallGroupIds: List<String>): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(courses: List<CourseEntity>)
}
