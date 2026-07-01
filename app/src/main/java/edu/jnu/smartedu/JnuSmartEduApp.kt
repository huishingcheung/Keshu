package edu.jnu.smartedu

import android.app.Application
import edu.jnu.smartedu.data.AppContainer
import edu.jnu.smartedu.widget.WidgetUpdateManager

class JnuSmartEduApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WidgetUpdateManager.scheduleDailyRefresh(this)
    }
}
