package com.keshu.mobile.data.repository

import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseStatus
import com.keshu.mobile.data.local.entity.SmallGroupEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdvisorFormattingTest {
    @Test
    fun matchesEquivalentAcademicTermFormats() {
        assertTrue("2025-2026学年 第2学期".matchesAcademicTerm("2025-2026-2"))
        assertFalse("2025-2026学年 第1学期".matchesAcademicTerm("2025-2026-2"))
        assertEquals("2025-2026学年 第2学期", "2025-2026-2".toDisplayTerm())
    }

    @Test
    fun convertsUnexpectedJsonToReadableSummary() {
        assertEquals("优先完成必修课，再补足课群缺口。", """{"summary":"优先完成必修课，再补足课群缺口。"}""".toReadableAiSummary())
        assertEquals(
            "DeepSeek 已完成分析，请结合下方课程卡片安排选课。",
            """{"targetTerm":"2025-2026-2","id":"debug"}""".toReadableAiSummary(),
        )
    }

    @Test
    fun choosesNextAcademicTermFromCurrentDate() {
        assertEquals("2026-2027-1", nextAcademicTermKey(LocalDate.of(2026, 7, 1)))
        assertEquals("2026-2027-2", nextAcademicTermKey(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun takingCreditsRollUpToParentGroupsWithoutDuplicates() {
        val groups = listOf(
            SmallGroupEntity("parent", "large", null, "父课群", 10.0),
            SmallGroupEntity("child", "large", "parent", "子课群", 5.0),
        )
        val sessionsOfSameCourse = listOf(
            CourseEntity("1", "C001", "在修课", 3.0, "child", status = CourseStatus.TAKING),
            CourseEntity("2", "C001", "在修课", 3.0, "child", status = CourseStatus.TAKING),
        )

        val credits = projectTakingCreditsByGroup(groups, sessionsOfSameCourse)

        assertEquals(3.0, credits["child"] ?: 0.0, 0.0)
        assertEquals(3.0, credits["parent"] ?: 0.0, 0.0)
    }
}
