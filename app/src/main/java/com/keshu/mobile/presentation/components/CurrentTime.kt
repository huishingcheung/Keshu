package com.keshu.mobile.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.LocalDateTime
import kotlinx.coroutines.delay

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * Emits the current local date and time and refreshes it on every minute boundary.
 *
 * The home screen decides which class is running and the schedule highlights today's column, so both
 * have to follow the clock while the screen stays open. Reading the clock inside `remember` froze
 * the value until unrelated data changed.
 *
 * The effect restarts on every resume so the first frame after a pause is correct.
 */
@Composable
internal fun rememberCurrentDateTime(): State<LocalDateTime> {
    val current = remember { mutableStateOf(LocalDateTime.now()) }
    var resumeCount by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeCount++ }
    LaunchedEffect(resumeCount) {
        while (true) {
            current.value = LocalDateTime.now()
            delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
        }
    }
    return current
}
