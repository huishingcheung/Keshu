package com.keshu.mobile.data.update

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The update check reads the manifest the project site publishes rather than `api.github.com`,
 * because that host is unreachable on many mainland networks. These tests pin the manifest
 * handling, including that the download link it names stays on the site's own origin.
 */
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

    @Test
    fun `the manifest download path resolves against the manifest url`() {
        val manifest = UpdateManifest(
            version = "0.4.5",
            apk = "downloads/Keshu-v0.4.5-preview.apk",
            releasePage = "https://github.com/huishingcheung/Keshu/releases/tag/v0.4.5",
        )

        val update = requireNotNull(manifest.toReleaseInfo(AppUpdateChecker.MANIFEST_URL)).toAppUpdate()

        assertEquals("0.4.5", update.version)
        assertEquals(
            "https://huishingcheung.github.io/Keshu/downloads/Keshu-v0.4.5-preview.apk",
            update.downloadUrl,
        )
        assertEquals(
            "https://github.com/huishingcheung/Keshu/releases/tag/v0.4.5",
            update.releasePageUrl,
        )
    }

    @Test
    fun `a manifest without a mirror falls back to the release page`() {
        val update = requireNotNull(
            UpdateManifest(version = "0.4.5").toReleaseInfo(AppUpdateChecker.MANIFEST_URL),
        ).toAppUpdate()

        assertEquals(AppUpdateChecker.DEFAULT_RELEASE_PAGE_URL, update.downloadUrl)
        assertEquals(AppUpdateChecker.DEFAULT_RELEASE_PAGE_URL, update.releasePageUrl)
    }

    @Test
    fun `a manifest without a usable version is rejected`() {
        assertNull(UpdateManifest(apk = "downloads/x.apk").toReleaseInfo(AppUpdateChecker.MANIFEST_URL))
        assertNull(UpdateManifest(version = "  ").toReleaseInfo(AppUpdateChecker.MANIFEST_URL))
    }

    @Test
    fun `an absolute mirror path is left untouched`() {
        val update = requireNotNull(
            UpdateManifest(
                version = "v0.4.5",
                apk = "https://example.test/Keshu.apk",
            ).toReleaseInfo(AppUpdateChecker.MANIFEST_URL),
        ).toAppUpdate()

        assertEquals("0.4.5", update.version)
        assertEquals("https://example.test/Keshu.apk", update.downloadUrl)
    }

    @Test
    fun `the github fallback prefers the apk asset`() {
        val release = GitHubRelease(
            tagName = "v0.4.5",
            htmlUrl = "https://github.com/huishingcheung/Keshu/releases/tag/v0.4.5",
            assets = listOf(
                GitHubAsset("https://github.com/huishingcheung/Keshu/releases/download/v0.4.5/notes.txt"),
                GitHubAsset("https://github.com/huishingcheung/Keshu/releases/download/v0.4.5/Keshu-v0.4.5-preview.apk"),
            ),
        )

        val update = requireNotNull(release.toReleaseInfo()).toAppUpdate()

        assertEquals("0.4.5", update.version)
        assertTrue(update.downloadUrl.endsWith("Keshu-v0.4.5-preview.apk"))
    }

    @Test
    fun `a github release without assets keeps the release page`() {
        val release = GitHubRelease(tagName = "v0.4.5")

        val update = requireNotNull(release.toReleaseInfo()).toAppUpdate()

        assertEquals(AppUpdateChecker.DEFAULT_RELEASE_PAGE_URL, update.downloadUrl)
    }

    @Test
    fun `an unparseable manifest url does not crash the check`() {
        val manifest = UpdateManifest(version = "0.4.5", apk = "downloads/x.apk")

        val update = requireNotNull(manifest.toReleaseInfo("not a url")).toAppUpdate()

        assertEquals(AppUpdateChecker.DEFAULT_RELEASE_PAGE_URL, update.downloadUrl)
    }
}
