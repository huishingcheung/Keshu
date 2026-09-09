package com.keshu.mobile.data.update

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {
    @Test
    fun `newer semantic versions are detected`() {
        assertTrue(isNewerVersion("v0.4.2", "0.4.1"))
        assertTrue(isNewerVersion("v0.10.0", "0.9.9"))
        assertTrue(isNewerVersion("v1.0", "0.99.99"))
    }

    @Test
    fun `same older and malformed versions are ignored`() {
        assertFalse(isNewerVersion("v0.4.2", "0.4.2"))
        assertFalse(isNewerVersion("v0.4.1", "0.4.2"))
        assertFalse(isNewerVersion("latest", "0.4.2"))
    }

    @Test
    fun `missing version parts are treated as zero`() {
        assertTrue(isNewerVersion("v0.4.2", "0.4"))
        assertFalse(isNewerVersion("v0.4", "0.4.0"))
    }

    @Test
    fun `successful checks are due once every three days`() {
        val lastCheck = 1_000L

        assertFalse(isUpdateCheckDue(lastCheck + TimeUnit.DAYS.toMillis(3) - 1L, lastCheck))
        assertTrue(isUpdateCheckDue(lastCheck + TimeUnit.DAYS.toMillis(3), lastCheck))
        assertTrue(isUpdateCheckDue(lastCheck, 0L))
        assertTrue(isUpdateCheckDue(lastCheck, lastCheck + 1L))
    }
}
