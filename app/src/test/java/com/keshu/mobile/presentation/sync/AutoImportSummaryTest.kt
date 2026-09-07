package com.keshu.mobile.presentation.sync

import com.keshu.mobile.data.local.ExamAvailability
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoImportSummaryTest {
    @Test
    fun includesImportedExamCount() {
        val summary = AutoImportSummary(
            groups = 12,
            courses = 48,
            schedule = 6,
            exams = ExamImportOutcome.Imported(2),
        )

        assertEquals("一键同步完成：课群12 课程48 课表6 考试2", summary.toUserMessage())
    }

    @Test
    fun explainsWhenExamImportIsUnavailable() {
        val summary = AutoImportSummary(
            groups = 12,
            courses = 48,
            schedule = 6,
            exams = ExamImportOutcome.Unavailable("2023-09-01 18:52:00–2026-07-26 18:52:00"),
        )

        assertEquals(
            "一键同步完成：课群12 课程48 课表6；考试安排当前不可查看" +
                "（可查看时间：2023-09-01 18:52:00–2026-07-26 18:52:00）",
            summary.toUserMessage(),
        )
    }

    @Test
    fun fallsBackToUnavailableWithoutInventingZeroExams() {
        val summary = AutoImportSummary(
            groups = 12,
            courses = 48,
            schedule = 6,
            exams = ExamImportOutcome.Unavailable(),
        )

        assertEquals(
            "一键同步完成：课群12 课程48 课表6；考试安排当前不可查看",
            summary.toUserMessage(),
        )
    }

    @Test
    fun reportsExamSyncFailureSeparatelyFromUnavailableState() {
        val summary = AutoImportSummary(
            groups = 12,
            courses = 48,
            schedule = 6,
            exams = ExamImportOutcome.Failed("页面加载超时"),
        )

        assertEquals(
            "一键同步完成：课群12 课程48 课表6；考试安排同步失败，请稍后重试",
            summary.toUserMessage(),
        )
    }

    @Test
    fun showsTheSchoolVisibilityRangeInTheEmptyState() {
        val availability = ExamAvailability("2023-09-01 18:52:00–2026-07-26 18:52:00")

        assertEquals(
            "学校开放的可查看时间范围：2023-09-01 18:52:00–2026-07-26 18:52:00",
            availability.detailMessage(),
        )
    }
}
