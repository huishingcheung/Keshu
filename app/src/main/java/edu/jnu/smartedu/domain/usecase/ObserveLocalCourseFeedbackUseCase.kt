package edu.jnu.smartedu.domain.usecase

import edu.jnu.smartedu.data.local.dao.CourseDao
import edu.jnu.smartedu.data.local.dao.CourseFeedbackStatsDao
import edu.jnu.smartedu.data.local.entity.CourseEntity
import edu.jnu.smartedu.data.local.entity.CourseFeedbackStatsEntity
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
