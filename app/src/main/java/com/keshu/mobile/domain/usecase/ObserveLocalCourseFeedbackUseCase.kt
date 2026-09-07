package com.keshu.mobile.domain.usecase

import com.keshu.mobile.data.local.dao.CourseDao
import com.keshu.mobile.data.local.dao.CourseFeedbackStatsDao
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseFeedbackStatsEntity
import kotlinx.coroutines.flow.Flow

class ObserveLocalCourseFeedbackUseCase(
    private val courseDao: CourseDao,
    private val feedbackDao: CourseFeedbackStatsDao,
) {
    fun observeCourses(): Flow<List<CourseEntity>> = courseDao.observeAll()

    fun observeFeedback(): Flow<List<CourseFeedbackStatsEntity>> = feedbackDao.observeAll()

    suspend fun save(
        course: CourseEntity,
        teacher: String?,
        avgScore: Double?,
        easiness: Int,
        workload: Int,
        recommendCount: Int,
        feedbackCount: Int,
    ) {
        feedbackDao.upsertAll(
            listOf(
                CourseFeedbackStatsEntity(
                    id = "${course.code}-${teacher.orEmpty()}",
                    courseCode = course.code,
                    courseName = course.name,
                    teacher = teacher?.ifBlank { null },
                    avgScore = avgScore,
                    easiness = easiness.coerceIn(1, 5),
                    workload = workload.coerceIn(1, 5),
                    recommendCount = recommendCount.coerceAtLeast(0),
                    feedbackCount = feedbackCount.coerceAtLeast(1),
                    source = "local_manual",
                ),
            ),
        )
    }
}
