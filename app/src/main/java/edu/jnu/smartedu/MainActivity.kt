package edu.jnu.smartedu

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import edu.jnu.smartedu.presentation.JnuSmartEduAppScreen
import edu.jnu.smartedu.presentation.theme.JnuSmartEduTheme
import kotlinx.coroutines.flow.MutableStateFlow

private data class AppLaunchRequest(
    val destination: String? = null,
    val addTask: Boolean = false,
    val id: Long = 0L,
)

class MainActivity : ComponentActivity() {
    private val launchRequest = MutableStateFlow(AppLaunchRequest())
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestImportantPermissions()
        handleIntent(intent)
        val container = (application as JnuSmartEduApp).container
        setContent {
            val request by launchRequest.collectAsState()
            JnuSmartEduTheme {
                JnuSmartEduAppScreen(
                    container = container,
                    launchDestination = request.destination,
                    launchAddTask = request.addTask,
                    launchRequestId = request.id,
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

    private fun requestImportantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_ADD_TASK = "add_task"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_EXAM = "exam"
    }
}
