package com.keshu.mobile.presentation.components

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Teaching weeks are counted Monday to Sunday, matching the timetable grid, which lays its columns
 * out from the Monday of the selected week. The semester start date is therefore only the anchor
 * for "which week contains this date", never the start of a rolling seven-day block.
 */
class SemesterWeekTest {
    @Test
    fun jnuFallSemesterStartsInWeekOneAndAdvancesOnMonday() {
        val start = "2026-09-07"

        assertEquals(1, weekFromSemesterStart(start, LocalDate.of(2026, 9, 7)))
        assertEquals(1, weekFromSemesterStart(start, LocalDate.of(2026, 9, 13)))
        assertEquals(2, weekFromSemesterStart(start, LocalDate.of(2026, 9, 14)))
        assertEquals(2, weekFromSemesterStart(start, LocalDate.of(2026, 9, 20)))
        assertEquals(3, weekFromSemesterStart(start, LocalDate.of(2026, 9, 21)))
    }

    @Test
    fun aMidWeekStartStillTurnsOverOnMonday() {
        // 2026-09-01 is a Tuesday. Its week runs from Monday 2026-08-31 to Sunday 2026-09-06.
        val start = "2026-09-01"

        assertEquals(1, weekFromSemesterStart(start, LocalDate.of(2026, 9, 1)))
        assertEquals(1, weekFromSemesterStart(start, LocalDate.of(2026, 9, 6)))
        assertEquals(2, weekFromSemesterStart(start, LocalDate.of(2026, 9, 7)))
    }

    @Test
    fun aSundayStartBelongsToTheWeekThatEndsOnIt() {
        // 2026-09-06 is a Sunday, so the very next day already begins week two.
        val start = "2026-09-06"

        assertEquals(1, weekFromSemesterStart(start, LocalDate.of(2026, 9, 6)))
        assertEquals(2, weekFromSemesterStart(start, LocalDate.of(2026, 9, 7)))
    }

    @Test
    fun datesBeforeTheSemesterStayInTheFirstWeek() {
        assertEquals(1, weekFromSemesterStart("2026-09-07", LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun anUnparseableStartDateHasNoWeek() {
        assertNull(weekFromSemesterStart("", LocalDate.of(2026, 9, 10)))
        assertNull(weekFromSemesterStart("2026/09/07", LocalDate.of(2026, 9, 10)))
    }

    @Test
    fun theDefaultStartIsTheFirstMondayOfTheTeachingMonth() {
        assertEquals("2026-09-07", defaultSemesterStartDate(LocalDate.of(2026, 9, 10)))
        assertEquals("2026-03-02", defaultSemesterStartDate(LocalDate.of(2026, 3, 1)))
        assertEquals(
            DayOfWeek.MONDAY,
            LocalDate.parse(defaultSemesterStartDate(LocalDate.of(2025, 9, 1))).dayOfWeek,
        )
    }
}
