package com.keshu.mobile.domain

import com.keshu.mobile.data.local.entity.ClassSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The week-range parser used to exist twice: the UI stopped its open-ended fallback at week 20 while
 * the widget used 30, so a `全周` course silently disappeared from the UI after week 20.
 */
class ScheduleWeeksTest {
    @Test
    fun explicitRangesSingleWeeksAndFiltersAreParsed() {
        assertEquals(setOf(1, 2, 3, 4), session("1-4周").explicitWeekNumbers())
        assertEquals(setOf(7), session("7周").explicitWeekNumbers())
        assertEquals(setOf(1, 3, 5, 7), session("1-7周 单周").explicitWeekNumbers())
        assertEquals(setOf(2, 4, 6), session("1-6周 双周").explicitWeekNumbers())
        assertEquals(setOf(1, 2, 3, 5, 6, 7), session("1-3周,5-7周").explicitWeekNumbers())
    }

    @Test
    fun openEndedAndUnreadableTextHasNoExplicitWeeks() {
        assertNull(session("").explicitWeekNumbers())
        assertNull(session("   ").explicitWeekNumbers())
        assertNull(session("全周").explicitWeekNumbers())
        assertNull(session("待定").explicitWeekNumbers())
    }

    @Test
    fun anOpenEndedCourseStillRunsInLaterWeeks() {
        // The regression: this used to stop at week 20, so week 21 and beyond hid the course.
        val weeks = session("全周").weekNumbers()

        assertTrue(25 in weeks)
        assertTrue(MAX_TEACHING_WEEKS in weeks)
        assertTrue(1 in weeks)
    }

    @Test
    fun explicitWeeksDoNotExpandToTheWholeTerm() {
        val weeks = session("1-4周").weekNumbers()

        assertEquals(setOf(1, 2, 3, 4), weeks)
        assertFalse(5 in weeks)
    }

    @Test
    fun anUnknownWeekMatchesAnyCourse() {
        assertTrue(session("1-4周").occursInWeek(null))
        assertTrue(session("1-4周").occursInWeek(3))
        assertFalse(session("1-4周").occursInWeek(5))
    }

    private fun session(weeks: String) = ClassSessionEntity(
        id = "session",
        term = "2026-2027-1",
        courseCode = "code",
        courseName = "course",
        teacher = "",
        credit = null,
        weeksText = weeks,
        dayOfWeek = 1,
        startSection = 1,
        endSection = 2,
        location = "",
        className = "",
    )
}
