package com.keshu.mobile.data.portal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.keshu.mobile.data.source.AcademicDataset

class JnuPortalScriptsTest {
    @Test
    fun `portal requests are asynchronous and credential scoped`() {
        val scripts = listOf(JnuCurriculumScripts.exporter, JnuScheduleScripts.exporter)

        scripts.forEach { script ->
            assertTrue(script.contains("await fetch"))
            assertTrue(script.contains("credentials: 'include'"))
            assertFalse(script.contains("XMLHttpRequest"))
            assertFalse(script.contains("doSyncAjax"))
        }
    }

    @Test
    fun `curriculum requests use a bounded worker pool`() {
        val script = JnuCurriculumScripts.exporter

        assertTrue(script.contains("Math.min(4, leaves.length)"))
        assertTrue(script.contains("Promise.all"))
    }

    @Test
    fun `data source contract covers all required datasets`() {
        val datasets = AcademicDataset.entries

        assertTrue(datasets.contains(AcademicDataset.CURRICULUM))
        assertTrue(datasets.contains(AcademicDataset.SCHEDULE))
        assertTrue(datasets.contains(AcademicDataset.EXAMS))
    }
}
