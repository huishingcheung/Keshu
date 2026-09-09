package com.keshu.mobile.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassTimePreferencesTest {
    @Test
    fun `huaqiao and panyu is the default profile with corrected evening periods`() {
        assertEquals(ClassTimeProfile.HUAQIAO_PANYU, ClassTimeProfile.fromId(null))
        assertEquals(ClassPeriodTime("08:30", "09:15"), ClassTimePresets.huaqiaoPanyu[0])
        assertEquals(ClassPeriodTime("12:20", "13:05"), ClassTimePresets.huaqiaoPanyu[4])
        assertEquals(ClassPeriodTime("18:30", "19:15"), ClassTimePresets.huaqiaoPanyu[9])
        assertEquals(ClassPeriodTime("19:25", "20:10"), ClassTimePresets.huaqiaoPanyu[10])
        assertEquals(ClassPeriodTime("20:20", "21:05"), ClassTimePresets.huaqiaoPanyu[11])
    }

    @Test
    fun `campus presets include their published day ranges`() {
        assertEquals(13, ClassTimePresets.mainZhuhai.size)
        assertEquals(ClassPeriodTime("20:50", "21:35"), ClassTimePresets.mainZhuhai[12])
        assertEquals(12, ClassTimePresets.shenzhen.size)
        assertEquals(ClassPeriodTime("18:00", "18:45"), ClassTimePresets.shenzhen[8])
        assertEquals(ClassPeriodTime("20:45", "21:30"), ClassTimePresets.shenzhen[11])
    }

    @Test
    fun `custom periods survive preference serialization`() {
        val periods = listOf(
            ClassPeriodTime("07:45", "08:30"),
            ClassPeriodTime("08:40", "09:25"),
        )

        assertEquals(periods, decodePeriods(encodePeriods(periods)))
    }

    @Test
    fun `invalid custom periods are rejected`() {
        assertFalse(isValidClassPeriod(ClassPeriodTime("24:00", "24:45")))
        assertFalse(isValidClassPeriod(ClassPeriodTime("09:15", "08:30")))
        assertNull(decodePeriods("09:15,08:30"))
        assertTrue(isValidClassPeriod(ClassPeriodTime("08:30", "09:15")))
    }
}
