package com.keshu.mobile.data.update

import android.content.SharedPreferences
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class AppUpdate(
    val version: String,
    /** Primary target for the download action. */
    val downloadUrl: String,
    /** The GitHub Release page, kept as a secondary reference. */
    val releasePageUrl: String,
)

/**
 * Outcome of a user-triggered update check. [AppUpdateChecker.checkIfDue] stays silent on "no
 * update" and on failure because it runs unattended; a manual check has to distinguish them.
 */
sealed interface UpdateCheckResult {
    /** A release newer than the installed version is available. */
    data class Available(val update: AppUpdate) : UpdateCheckResult

    /** The check succeeded and the installed version is the newest release. */
    data object UpToDate : UpdateCheckResult

    /** The check could not complete. The retry interval stays unchanged so a later attempt retries. */
    data object Failed : UpdateCheckResult
}

class AppUpdateChecker(
    private val client: OkHttpClient,
    moshi: Moshi,
    private val preferences: SharedPreferences,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val manifestAdapter = moshi.adapter(UpdateManifest::class.java)
    private val releaseAdapter = moshi.adapter(GitHubRelease::class.java)
    private val mutex = Mutex()

    suspend fun checkIfDue(currentVersion: String): AppUpdate? = mutex.withLock {
        val now = nowMillis()
        val lastSuccessfulCheck = preferences.getLong(KEY_LAST_SUCCESSFUL_CHECK, 0L)
        if (!isUpdateCheckDue(now, lastSuccessfulCheck)) return null

        val release = fetchRelease() ?: return null
        preferences.edit().putLong(KEY_LAST_SUCCESSFUL_CHECK, now).apply()
        if (!isNewerVersion(release.version, currentVersion)) return null

        release.toAppUpdate()
    }

    /**
     * Runs a check on demand. It ignores the retry interval and reports the outcome explicitly so
     * the UI can tell "already newest" apart from "check failed".
     */
    suspend fun checkNow(currentVersion: String): UpdateCheckResult = mutex.withLock {
        val now = nowMillis()
        val release = fetchRelease() ?: return UpdateCheckResult.Failed
        preferences.edit().putLong(KEY_LAST_SUCCESSFUL_CHECK, now).apply()
        if (!isNewerVersion(release.version, currentVersion)) return UpdateCheckResult.UpToDate

        UpdateCheckResult.Available(release.toAppUpdate())
    }

    /**
     * Asks the project site first. `api.github.com` is unreachable on many mainland networks while
     * the site the APK is already downloaded from is not, and the manifest also names that mirror so
     * the download action can avoid `github.com`. The GitHub API stays as a fallback for wherever
     * the site cannot be reached, and for a deployment that predates the manifest.
     */
    private suspend fun fetchRelease(): ReleaseInfo? = fetchManifest() ?: fetchGitHubRelease()

    private suspend fun fetchManifest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(MANIFEST_URL)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
                    ?.let(manifestAdapter::fromJson)
                    ?.toReleaseInfo(baseUrl = MANIFEST_URL)
            }
        }.getOrNull()
    }

    private suspend fun fetchGitHubRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_API_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
                    ?.let(releaseAdapter::fromJson)
                    ?.toReleaseInfo()
            }
        }.getOrNull()
    }

    companion object {
        private const val KEY_LAST_SUCCESSFUL_CHECK = "last_successful_update_check"
        private const val USER_AGENT = "Keshu-Android"

        /** Published by the Pages deployment on every release. */
        const val MANIFEST_URL = "https://huishingcheung.github.io/Keshu/latest.json"

        internal const val DEFAULT_RELEASE_PAGE_URL =
            "https://github.com/huishingcheung/Keshu/releases/latest"

        private const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/huishingcheung/Keshu/releases/latest"
    }
}

/**
 * The manifest the project site publishes at [AppUpdateChecker.MANIFEST_URL] on every deployment.
 * Paths in it are relative so the manifest survives a change of domain.
 */
internal data class UpdateManifest(
    @Json(name = "version") val version: String? = null,
    @Json(name = "apk") val apk: String? = null,
    @Json(name = "releasePage") val releasePage: String? = null,
)

/** A release as reported by whichever source answered. */
internal data class ReleaseInfo(
    val version: String,
    val downloadUrl: String?,
    val releasePageUrl: String?,
)

internal fun UpdateManifest.toReleaseInfo(baseUrl: String): ReleaseInfo? {
    val resolvedVersion = version?.trim()?.removePrefix("v")?.takeIf(String::isNotBlank) ?: return null
    return ReleaseInfo(
        version = resolvedVersion,
        downloadUrl = apk?.trim()?.takeIf(String::isNotBlank)?.let { resolveAgainst(baseUrl, it) },
        releasePageUrl = releasePage?.trim()?.takeIf(String::isNotBlank),
    )
}

/** Resolves a possibly relative manifest path against the manifest's own URL. */
internal fun resolveAgainst(baseUrl: String, path: String): String? {
    return runCatching { URI(baseUrl).resolve(path).toString() }.getOrNull()
}

/** Prefers the mirrored APK, and falls back to the Release page when no mirror was named. */
internal fun ReleaseInfo.toAppUpdate(): AppUpdate {
    val page = releasePageUrl ?: AppUpdateChecker.DEFAULT_RELEASE_PAGE_URL
    return AppUpdate(
        version = version,
        downloadUrl = downloadUrl ?: page,
        releasePageUrl = page,
    )
}

internal data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "assets") val assets: List<GitHubAsset> = emptyList(),
) {
    fun toReleaseInfo(): ReleaseInfo? {
        val resolvedVersion = tagName?.trim()?.removePrefix("v")?.takeIf(String::isNotBlank) ?: return null
        return ReleaseInfo(
            version = resolvedVersion,
            downloadUrl = assets
                .firstOrNull { it.browserDownloadUrl.endsWith(".apk", ignoreCase = true) }
                ?.browserDownloadUrl,
            releasePageUrl = htmlUrl,
        )
    }
}

internal data class GitHubAsset(
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
)

internal fun isUpdateCheckDue(nowMillis: Long, lastSuccessfulCheckMillis: Long): Boolean {
    if (lastSuccessfulCheckMillis <= 0L || nowMillis < lastSuccessfulCheckMillis) return true
    return nowMillis - lastSuccessfulCheckMillis >= TimeUnit.DAYS.toMillis(3)
}

internal fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    val latest = latestVersion.toVersionParts() ?: return false
    val current = currentVersion.toVersionParts() ?: return false
    val size = maxOf(latest.size, current.size)
    repeat(size) { index ->
        val latestPart = latest.getOrElse(index) { 0 }
        val currentPart = current.getOrElse(index) { 0 }
        if (latestPart != currentPart) return latestPart > currentPart
    }
    return false
}

private fun String.toVersionParts(): List<Int>? {
    val value = trim().removePrefix("v").substringBefore('-')
    if (value.isBlank()) return null
    return value.split('.').map { it.toIntOrNull() ?: return null }
}
