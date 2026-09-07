package com.keshu.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keshu.mobile.data.local.entity.LargeGroupEntity
import com.keshu.mobile.data.local.entity.AcademicProgressEntity
import com.keshu.mobile.data.local.entity.SmallGroupEntity
import com.keshu.mobile.data.local.projection.GraduationProgressRow
import com.keshu.mobile.data.local.projection.LargeGroupProgressRow
import com.keshu.mobile.data.local.projection.SmallGroupProgressRow
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM large_groups ORDER BY sortOrder, name")
    fun observeLargeGroups(): Flow<List<LargeGroupEntity>>

    @Query("SELECT * FROM small_groups ORDER BY sortOrder, name")
    fun observeSmallGroups(): Flow<List<SmallGroupEntity>>

    @Query("SELECT * FROM academic_progress WHERE id = 'graduation' LIMIT 1")
    fun observeAcademicProgress(): Flow<AcademicProgressEntity?>

    @Query(
        """
        SELECT
          sg.id AS groupId,
          sg.largeGroupId AS largeGroupId,
          sg.parentSmallGroupId AS parentSmallGroupId,
          sg.depth AS depth,
          sg.name AS name,
          sg.requiredCredits AS requiredCredits,
          COALESCE(SUM(CASE WHEN c.status = 'PASSED' THEN c.credit ELSE 0 END), 0) AS earnedCredits,
          CASE WHEN COALESCE(SUM(CASE WHEN c.status = 'PASSED' THEN c.credit ELSE 0 END), 0) >= sg.requiredCredits THEN 1 ELSE 0 END AS met
        FROM small_groups sg
        LEFT JOIN courses c ON c.smallGroupId = sg.id
        GROUP BY sg.id
        ORDER BY sg.sortOrder, sg.name
        """,
    )
    fun observeSmallGroupProgress(): Flow<List<SmallGroupProgressRow>>

    @Query(
        """
        SELECT
          lg.id AS groupId,
          lg.name AS name,
          lg.requiredCredits AS requiredCredits,
          COALESCE(SUM(CASE WHEN c.status = 'PASSED' THEN c.credit ELSE 0 END), 0) AS earnedCredits,
          COUNT(DISTINCT sg.id) AS smallGroupCount,
          COUNT(DISTINCT CASE WHEN COALESCE(sp.earnedCredits, 0) >= sg.requiredCredits THEN sg.id END) AS metSmallGroupCount,
          CASE
            WHEN COALESCE(SUM(CASE WHEN c.status = 'PASSED' THEN c.credit ELSE 0 END), 0) >= lg.requiredCredits
             AND COUNT(DISTINCT sg.id) = COUNT(DISTINCT CASE WHEN COALESCE(sp.earnedCredits, 0) >= sg.requiredCredits THEN sg.id END)
            THEN 1 ELSE 0
          END AS met
        FROM large_groups lg
        LEFT JOIN small_groups sg ON sg.largeGroupId = lg.id
        LEFT JOIN courses c ON c.smallGroupId = sg.id
        LEFT JOIN (
          SELECT smallGroupId, SUM(CASE WHEN status = 'PASSED' THEN credit ELSE 0 END) AS earnedCredits
          FROM courses
          GROUP BY smallGroupId
        ) sp ON sp.smallGroupId = sg.id
        GROUP BY lg.id
        ORDER BY lg.sortOrder, lg.name
        """,
    )
    fun observeLargeGroupProgress(): Flow<List<LargeGroupProgressRow>>

    @Query(
        """
        SELECT
          :requiredCredits AS requiredCredits,
          COALESCE(SUM(CASE WHEN c.status = 'PASSED' THEN c.credit ELSE 0 END), 0) AS earnedCredits,
          COUNT(DISTINCT lg.id) AS largeGroupCount,
          COUNT(DISTINCT CASE WHEN lp.met = 1 THEN lg.id END) AS metLargeGroupCount,
          CASE
            WHEN COALESCE(SUM(CASE WHEN c.status = 'PASSED' THEN c.credit ELSE 0 END), 0) >= :requiredCredits
             AND COUNT(DISTINCT lg.id) = COUNT(DISTINCT CASE WHEN lp.met = 1 THEN lg.id END)
            THEN 1 ELSE 0
          END AS met
        FROM large_groups lg
        LEFT JOIN small_groups sg ON sg.largeGroupId = lg.id
        LEFT JOIN courses c ON c.smallGroupId = sg.id
        LEFT JOIN (
          SELECT
            inner_lg.id AS largeGroupId,
            CASE
              WHEN COALESCE(SUM(CASE WHEN inner_c.status = 'PASSED' THEN inner_c.credit ELSE 0 END), 0) >= inner_lg.requiredCredits
               AND COUNT(DISTINCT inner_sg.id) = COUNT(DISTINCT CASE WHEN COALESCE(inner_sp.earnedCredits, 0) >= inner_sg.requiredCredits THEN inner_sg.id END)
              THEN 1 ELSE 0
            END AS met
          FROM large_groups inner_lg
          LEFT JOIN small_groups inner_sg ON inner_sg.largeGroupId = inner_lg.id
          LEFT JOIN courses inner_c ON inner_c.smallGroupId = inner_sg.id
          LEFT JOIN (
            SELECT smallGroupId, SUM(CASE WHEN status = 'PASSED' THEN credit ELSE 0 END) AS earnedCredits
            FROM courses
            GROUP BY smallGroupId
          ) inner_sp ON inner_sp.smallGroupId = inner_sg.id
          GROUP BY inner_lg.id
        ) lp ON lp.largeGroupId = lg.id
        """,
    )
    fun observeGraduationProgress(requiredCredits: Double): Flow<GraduationProgressRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLargeGroups(groups: List<LargeGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSmallGroups(groups: List<SmallGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAcademicProgress(progress: AcademicProgressEntity)
}
