package com.keshu.mobile.presentation.home

import com.keshu.mobile.data.local.ClassTimeProfile
import com.keshu.mobile.data.local.ClassTimeSettings
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The home screen used to keep reporting classes that had already finished, because the current
 * time was read once inside a `remember` block and the "remaining" list was never filtered by the
 * clock. These tests pin the ordering decisions to an explicit time value.
 */
class TodayCourseTimelineTest {
    private val settings = ClassTimeSettings(ClassTimeProfile.HUAQIAO_PANYU)

    @Test
    fun finishedClassIsNotOfferedAsTheNextOne() {
        val plan = resolveTodayCourses(
            sessions = listOf(session("morning", startSection = 1, endSection = 1)),
            settings = settings,
            nowMinutes = minutes(10, 10),
        )

        assertNull(plan.inProgress)
        assertNull(plan.next)
        assertNull(plan.highlight)
        assertTrue(plan.remaining.isEmpty())
    }

    @Test
    fun runningClassIsReportedAsInProgressWithRemainingMinutes() {
        val plan = resolveTodayCourses(
            sessions = listOf(session("running", startSection = 2, endSection = 2)),
            settings = settings,
            nowMinutes = minutes(9, 50),
        )

        assertEquals("running", plan.inProgress?.id)
        assertNull(plan.next)
        assertEquals(20, plan.inProgressRemainingMinutes)
        assertEquals(25f / 45f, requireNotNull(plan.inProgressElapsedFraction), 0.0001f)
    }

    @Test
    fun classStartsExactlyAtTheCurrentMinute() {
        val plan = resolveTodayCourses(
            sessions = listOf(session("starting", startSection = 2, endSection = 2)),
            settings = settings,
            nowMinutes = minutes(9, 25),
        )

        assertEquals("starting", plan.inProgress?.id)
    }

    @Test
    fun classLeavesTheInProgressSlotOnItsFinalMinute() {
        val plan = resolveTodayCourses(
            sessions = listOf(session("ending", startSection = 2, endSection = 2)),
            settings = settings,
            nowMinutes = minutes(10, 10),
        )

        assertNull(plan.inProgress)
        assertNull(plan.next)
        assertTrue(plan.remaining.isEmpty())
    }

    @Test
    fun remainingDropsFinishedClassesAndTheHighlightedOne() {
        val plan = resolveTodayCourses(
            sessions = listOf(
                session("finished", startSection = 1, endSection = 1),
                session("next", startSection = 3, endSection = 3),
                session("later", startSection = 6, endSection = 6),
            ),
            settings = settings,
            nowMinutes = minutes(10, 0),
        )

        assertNull(plan.inProgress)
        assertEquals("next", plan.next?.id)
        assertEquals(listOf("later"), plan.remaining.map { it.id })
    }

    @Test
    fun everyLaterClassStaysListedWhileOneIsRunning() {
        val plan = resolveTodayCourses(
            sessions = listOf(
                session("running", startSection = 2, endSection = 2),
                session("next", startSection = 3, endSection = 3),
                session("later", startSection = 6, endSection = 6),
            ),
            settings = settings,
            nowMinutes = minutes(9, 50),
        )

        assertEquals("running", plan.inProgress?.id)
        assertEquals("next", plan.next?.id)
        assertEquals(listOf("next", "later"), plan.remaining.map { it.id })
    }

    @Test
    fun sectionBeyondTheProfileExpiresInsteadOfStayingNext() {
        // The selected profile has 12 periods. A 13th-period course used to resolve to no end time
        // and was therefore reported as the next class at every hour of the day.
        val late = session("late", startSection = 13, endSection = 13)

        val beforeItEnds = resolveTodayCourses(listOf(late), settings, minutes(20, 40))
        assertEquals("late", beforeItEnds.inProgress?.id)

        val afterItEnds = resolveTodayCourses(listOf(late), settings, minutes(21, 30))
        assertNull(afterItEnds.inProgress)
        assertNull(afterItEnds.next)
        assertTrue(afterItEnds.remaining.isEmpty())
    }

    @Test
    fun emptyDayProducesNothingToShow() {
        val plan = resolveTodayCourses(emptyList(), settings, minutes(9, 0))

        assertNull(plan.highlight)
        assertTrue(plan.remaining.isEmpty())
    }

    private fun session(
        id: String,
        startSection: Int,
        endSection: Int,
    ) = ClassSessionEntity(
        id = id,
        term = "2026-2027-1",
        courseCode = id,
        courseName = id,
        teacher = "",
        credit = null,
        weeksText = "1-18周",
        dayOfWeek = 1,
        startSection = startSection,
        endSection = endSection,
        location = "",
        className = "",
    )

    private fun minutes(hour: Int, minute: Int): Int = hour * 60 + minute
}
