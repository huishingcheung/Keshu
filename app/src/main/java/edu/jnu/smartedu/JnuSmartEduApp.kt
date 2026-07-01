package edu.jnu.smartedu

import android.app.Application
import edu.jnu.smartedu.data.AppContainer

class JnuSmartEduApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
