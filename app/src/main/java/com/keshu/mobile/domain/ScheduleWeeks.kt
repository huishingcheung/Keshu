package com.keshu.mobile.domain

import com.keshu.mobile.data.local.entity.ClassSessionEntity

/** How many weeks the week picker offers when nothing more specific is known. */
const val DEFAULT_TEACHING_WEEKS = 20

/** Highest teaching week any supported semester can reach. */
const val MAX_TEACHING_WEEKS = 30

/**
 * The teaching weeks a session runs in, or null when its text means "every week".
 *
 * `weeksText` arrives from the portal in several shapes: `全周`, a single week, a range, and ranges
 * qualified by `单周` or `双周`. Callers that need to size a term should use this and fall back
 * themselves; [weekNumbers] expands the open-ended case, which would otherwise make every week
 * look explicitly scheduled.
 */
fun ClassSessionEntity.explicitWeekNumbers(): Set<Int>? {
    val text = weeksText.trim()
    if (text.isBlank() || text.contains("全周")) return null

    val oddOnly = text.contains("单")
    val evenOnly = text.contains("双")
    val weeks = linkedSetOf<Int>()
    Regex("""(\d{1,2})(?:\s*[-~－至]\s*(\d{1,2}))?""").findAll(text).forEach { match ->
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].takeIf(String::isNotBlank)?.toInt() ?: start
        weeks += (start.coerceAtMost(end)..start.coerceAtLeast(end))
            .filter { it in 1..MAX_TEACHING_WEEKS }
            .filter { !oddOnly || it % 2 == 1 }
            .filter { !evenOnly || it % 2 == 0 }
    }
    return weeks.ifEmpty { null }
}

/**
 * The teaching weeks a session runs in.
 *
 * An unreadable or open-ended value means every week: showing a course in a week it does not meet is
 * less harmful than hiding it from a week it does. The fallback reaches [MAX_TEACHING_WEEKS] because
 * the timetable once dropped an open-ended course from week 21 onward.
 */
fun ClassSessionEntity.weekNumbers(): Set<Int> {
    return explicitWeekNumbers() ?: (1..MAX_TEACHING_WEEKS).toSet()
}

/** True when [week] is unknown or the session runs in it. */
fun ClassSessionEntity.occursInWeek(week: Int?): Boolean {
    return week == null || week in weekNumbers()
}
