package com.keshu.mobile.data.update

import android.content.SharedPreferences
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class AppUpdate(
    val version: String,
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
    private val releaseAdapter = moshi.adapter(GitHubRelease::class.java)
    private val mutex = Mutex()

    suspend fun checkIfDue(currentVersion: String): AppUpdate? = mutex.withLock {
        val now = nowMillis()
        val lastSuccessfulCheck = preferences.getLong(KEY_LAST_SUCCESSFUL_CHECK, 0L)
        if (!isUpdateCheckDue(now, lastSuccessfulCheck)) return null

        val release = fetchLatestRelease() ?: return null
        preferences.edit().putLong(KEY_LAST_SUCCESSFUL_CHECK, now).apply()
        if (!isNewerVersion(release.tagName, currentVersion)) return null

        AppUpdate(
            version = release.tagName.removePrefix("v"),
            releasePageUrl = RELEASE_PAGE_URL,
        )
    }

    /**
     * Runs a check the user asked for. It ignores the retry interval and reports the outcome
     * explicitly so the UI can tell "already newest" apart from "check failed".
     */
    suspend fun checkNow(currentVersion: String): UpdateCheckResult = mutex.withLock {
        val now = nowMillis()
        val release = fetchLatestRelease() ?: return UpdateCheckResult.Failed
        preferences.edit().putLong(KEY_LAST_SUCCESSFUL_CHECK, now).apply()
        if (!isNewerVersion(release.tagName, currentVersion)) return UpdateCheckResult.UpToDate

        UpdateCheckResult.Available(
            AppUpdate(
                version = release.tagName.removePrefix("v"),
                releasePageUrl = RELEASE_PAGE_URL,
            ),
        )
    }

    private suspend fun fetchLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_API_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Keshu-Android")
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.let(releaseAdapter::fromJson)
            }
        }.getOrNull()
    }

    companion object {
        private const val KEY_LAST_SUCCESSFUL_CHECK = "last_successful_update_check"
        private const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/huishingcheung/Keshu/releases/latest"
        private const val RELEASE_PAGE_URL =
            "https://github.com/huishingcheung/Keshu/releases/latest"
    }
}

internal fun isUpdateCheckDue(nowMillis: Long, lastSuccessfulCheckMillis: Long): Boolean {
    if (lastSuccessfulCheckMillis <= 0L || nowMillis < lastSuccessfulCheckMillis) return true
    return nowMillis - lastSuccessfulCheckMillis >= TimeUnit.DAYS.toMillis(3)
}

internal data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String,
)

internal fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
    val latest = latestTag.toVersionParts() ?: return false
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
