package com.keshu.mobile

import android.app.Application
import com.keshu.mobile.data.AppContainer
import com.keshu.mobile.widget.WidgetUpdateManager

class KeshuApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WidgetUpdateManager.scheduleDailyRefresh(this)
        WidgetUpdateManager.requestUpdate(this)
    }
}
