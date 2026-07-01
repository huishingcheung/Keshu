package edu.jnu.smartedu.domain.usecase

import edu.jnu.smartedu.data.local.dao.ClassScheduleDao
import edu.jnu.smartedu.data.local.dao.CourseDao
import edu.jnu.smartedu.data.local.entity.ClassSessionEntity
import edu.jnu.smartedu.data.local.entity.CourseEntity
import edu.jnu.smartedu.data.local.entity.CourseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CreditSummary(
    val earnedCredits: Double = 0.0,
    val takingCredits: Double = 0.0,
    val projectedCredits: Double = 0.0,
    val currentTerm: String? = null,
)

class ObserveCreditSummaryUseCase(
    private val courseDao: CourseDao,
    private val classScheduleDao: ClassScheduleDao,
) {
    operator fun invoke(): Flow<CreditSummary> {
        return combine(
            courseDao.observeAll(),
            classScheduleDao.observeAll(),
            ::calculateCreditSummary,
        )
    }
}

fun calculateCreditSummary(
    detailCourses: List<CourseEntity>,
    classSessions: List<ClassSessionEntity>,
): CreditSummary {
    val earnedCredits = detailCourses
        .filter { it.status == CourseStatus.PASSED && it.smallGroupId != "small-unclassified-transcript" }
        .distinctBy { it.code.ifBlank { it.name } }
        .sumOf { it.credit }
    val currentTerm = classSessions.maxOfOrNull { it.term }
    val takingCredits = classSessions
        .filter { currentTerm == null || it.term == currentTerm }
        .distinctBy { it.courseCode.ifBlank { it.courseName } }
        .sumOf { it.credit ?: 0.0 }
    return CreditSummary(
        earnedCredits = earnedCredits,
        takingCredits = takingCredits,
        projectedCredits = earnedCredits + takingCredits,
        currentTerm = currentTerm,
    )
}
