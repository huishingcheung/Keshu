package com.keshu.mobile

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.keshu.mobile.presentation.KeshuAppScreen
import com.keshu.mobile.presentation.theme.KeshuTheme
import kotlinx.coroutines.flow.MutableStateFlow

private data class AppLaunchRequest(
    val destination: String? = null,
    val addTask: Boolean = false,
    val id: Long = 0L,
)

class MainActivity : ComponentActivity() {
    private val launchRequest = MutableStateFlow(AppLaunchRequest())
    private var pendingNotificationPermissionResult: ((Boolean) -> Unit)? = null
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingNotificationPermissionResult?.invoke(granted)
            pendingNotificationPermissionResult = null
            if (!granted) {
                Toast.makeText(this, "未开启通知权限，提醒不会发送", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        handleIntent(intent)
        val container = (application as KeshuApp).container
        setContent {
            val request by launchRequest.collectAsState()
            KeshuTheme {
                KeshuAppScreen(
                    container = container,
                    launchDestination = request.destination,
                    launchAddTask = request.addTask,
                    launchRequestId = request.id,
                    onAddTaskLaunchConsumed = ::consumeAddTaskRequest,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        launchRequest.value = AppLaunchRequest(
            destination = intent?.getStringExtra(EXTRA_DESTINATION),
            addTask = intent?.getBooleanExtra(EXTRA_ADD_TASK, false) == true,
            id = System.nanoTime(),
        )
    }

    private fun consumeAddTaskRequest(requestId: Long) {
        val current = launchRequest.value
        if (current.id != requestId || !current.addTask) return
        launchRequest.value = current.copy(addTask = false)
        intent?.removeExtra(EXTRA_ADD_TASK)
    }

    private fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
            return
        }
        pendingNotificationPermissionResult = onResult
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_ADD_TASK = "add_task"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_EXAM = "exam"
    }
}
