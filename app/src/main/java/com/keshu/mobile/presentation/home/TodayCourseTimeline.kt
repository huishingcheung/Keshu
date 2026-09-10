package com.keshu.mobile.presentation.home

import com.keshu.mobile.data.local.ClassTimeSettings
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.data.local.timelinePeriod
import com.keshu.mobile.data.local.toMinutesOfDay

/**
 * A single session placed on today's clock.
 *
 * [startMinutes] and [endMinutes] are minutes after midnight. They are null when the selected
 * campus profile cannot produce a valid time for the referenced section.
 */
internal data class TimedSession(
    val session: ClassSessionEntity,
    val startMinutes: Int?,
    val endMinutes: Int?,
)

/**
 * Today's sessions split for the home screen.
 *
 * [inProgress] is the class running right now, [next] is the first class that has not started
 * yet, and [remaining] holds the later classes that the hero card does not already present.
 */
internal data class TodayCoursePlan(
    val inProgress: ClassSessionEntity?,
    val inProgressRemainingMinutes: Int?,
    val inProgressElapsedFraction: Float?,
    val next: ClassSessionEntity?,
    val remaining: List<ClassSessionEntity>,
) {
    /** The single session the hero card presents. */
    val highlight: ClassSessionEntity?
        get() = inProgress ?: next
}

/**
 * Orders today's sessions against [nowMinutes] so finished classes stop being reported as
 * upcoming ones. The caller supplies the current time instead of reading the clock here, which
 * keeps this decision testable and independent of how often the UI recomposes.
 */
internal fun resolveTodayCourses(
    sessions: List<ClassSessionEntity>,
    settings: ClassTimeSettings,
    nowMinutes: Int,
): TodayCoursePlan {
    val timed = sessions
        .sortedBy { it.startSection }
        .map { session ->
            val start = settings.timelinePeriod(session.startSection)?.start?.toMinutesOfDay()
            val end = settings.timelinePeriod(session.endSection)?.end?.toMinutesOfDay() ?: start
            TimedSession(session, start, end)
        }

    val inProgress = timed.firstOrNull { it.isInProgress(nowMinutes) }
    val upcoming = timed.filter { it.startsAfter(nowMinutes) }
    val next = upcoming.firstOrNull()
    val highlightedId = (inProgress ?: next)?.session?.id

    return TodayCoursePlan(
        inProgress = inProgress?.session,
        inProgressRemainingMinutes = inProgress
            ?.endMinutes
            ?.let { (it - nowMinutes).coerceAtLeast(0) },
        inProgressElapsedFraction = inProgress?.elapsedFraction(nowMinutes),
        next = next?.session,
        remaining = upcoming
            .filterNot { it.session.id == highlightedId }
            .map { it.session },
    )
}

private fun TimedSession.elapsedFraction(nowMinutes: Int): Float? {
    val start = startMinutes ?: return null
    val end = endMinutes ?: return null
    val total = end - start
    if (total <= 0) return null
    return (nowMinutes - start).coerceIn(0, total).toFloat() / total
}

private fun TimedSession.isInProgress(nowMinutes: Int): Boolean {
    val start = startMinutes ?: return false
    val end = endMinutes ?: return false
    return start <= nowMinutes && nowMinutes < end
}

/**
 * A session whose section carries no time is treated as not started yet, so unexpected data
 * stays visible instead of silently disappearing from the day.
 */
private fun TimedSession.startsAfter(nowMinutes: Int): Boolean {
    val start = startMinutes ?: return true
    return start > nowMinutes
}
