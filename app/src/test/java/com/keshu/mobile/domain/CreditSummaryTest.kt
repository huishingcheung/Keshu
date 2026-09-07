package com.keshu.mobile.domain

import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseStatus
import com.keshu.mobile.domain.usecase.calculateCreditSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class CreditSummaryTest {
    @Test
    fun summaryUsesPassedDetailCoursesAndLatestScheduleWithoutDuplicates() {
        val detailCourses = listOf(
            course("t1", "A001", "已通过一", 3.0, CourseStatus.PASSED),
            course("t2", "A002", "已通过二", 2.0, CourseStatus.PASSED),
            course("t3", "A003", "未通过", 4.0, CourseStatus.FAILED),
            course("t4", "A004", "成绩单未归类", 6.0, CourseStatus.PASSED, "small-unclassified-transcript"),
        )
        val schedule = listOf(
            session("old", "2025-2026-1", "B001", 5.0, 1),
            session("new-1", "2025-2026-2", "C001", 3.0, 1),
            session("new-2", "2025-2026-2", "C001", 3.0, 3),
            session("new-3", "2025-2026-2", "C002", 2.0, 5),
        )

        val summary = calculateCreditSummary(detailCourses, schedule)

        assertEquals(5.0, summary.earnedCredits, 0.0)
        assertEquals(5.0, summary.takingCredits, 0.0)
        assertEquals(10.0, summary.projectedCredits, 0.0)
        assertEquals("2025-2026-2", summary.currentTerm)
    }

    private fun course(
        id: String,
        code: String,
        name: String,
        credit: Double,
        status: CourseStatus,
        smallGroupId: String = "small-detail",
    ): CourseEntity {
        return CourseEntity(id, code, name, credit, smallGroupId, status = status)
    }

    private fun session(id: String, term: String, code: String, credit: Double, day: Int): ClassSessionEntity {
        return ClassSessionEntity(
            id = id,
            term = term,
            courseCode = code,
            courseName = code,
            teacher = "",
            credit = credit,
            weeksText = "1-16周",
            dayOfWeek = day,
            startSection = 1,
            endSection = 2,
            location = "",
            className = "",
        )
    }
}
