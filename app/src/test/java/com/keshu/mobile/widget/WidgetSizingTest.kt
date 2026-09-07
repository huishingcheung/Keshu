package com.keshu.mobile.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizingTest {
    @Test
    fun taskWidgetRevealsRowsAsHeightGrows() {
        assertEquals(1, dashboardMaxRowsForHeight(130))
        assertEquals(2, dashboardMaxRowsForHeight(180))
        assertEquals(3, dashboardMaxRowsForHeight(240))
    }

    @Test
    fun compactSchedulePrioritizesExamInformation() {
        assertEquals(0, scheduleMaxCourseRowsForHeight(140, hasExamInfo = true))
        assertEquals(1, scheduleMaxCourseRowsForHeight(140, hasExamInfo = false))
        assertEquals(1, scheduleMaxCourseRowsForHeight(190, hasExamInfo = true))
        assertEquals(2, scheduleMaxCourseRowsForHeight(240, hasExamInfo = true))
        assertEquals(3, scheduleMaxCourseRowsForHeight(285, hasExamInfo = true))
    }
}
