package com.keshu.mobile.presentation.schedule

import com.keshu.mobile.data.local.entity.ClassSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleWeekIndexTest {
    @Test
    fun indexesWeekRangesOnceAndKeepsSessionsInDisplayOrder() {
        val index = buildScheduleWeekIndex(
            sessions = listOf(
                session(id = "late", weeks = "1-4周", day = 3, startSection = 5),
                session(id = "early", weeks = "1-4周", day = 1, startSection = 1),
                session(id = "odd", weeks = "1-7周 单周", day = 2, startSection = 3),
            ).sortedWith(compareBy<ClassSessionEntity> { it.dayOfWeek }.thenBy { it.startSection }),
            currentWeek = 22,
        )

        assertEquals((1..22).toList(), index.weeks)
        assertEquals(listOf("early", "odd", "late"), index.sessionsByWeek.getValue(1).map { it.id })
        assertEquals(listOf("early", "late"), index.sessionsByWeek.getValue(2).map { it.id })
        assertEquals(listOf("odd"), index.sessionsByWeek.getValue(5).map { it.id })
        assertTrue(index.sessionsByWeek.getValue(8).isEmpty())
    }

    @Test
    fun emptyScheduleStillOffersTheStandardTeachingWeeks() {
        val index = buildScheduleWeekIndex(emptyList(), currentWeek = null)

        assertEquals((1..20).toList(), index.weeks)
        assertTrue(index.sessionsByWeek.values.all { it.isEmpty() })
    }

    private fun session(
        id: String,
        weeks: String,
        day: Int,
        startSection: Int,
    ) = ClassSessionEntity(
        id = id,
        term = "2026-2027-1",
        courseCode = id,
        courseName = id,
        teacher = "",
        credit = null,
        weeksText = weeks,
        dayOfWeek = day,
        startSection = startSection,
        endSection = startSection + 1,
        location = "",
        className = "",
    )
}
