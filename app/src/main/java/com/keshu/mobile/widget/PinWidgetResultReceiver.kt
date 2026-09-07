package com.keshu.mobile.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PinWidgetResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val message = if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            "桌面没有返回小部件编号，请到桌面确认是否添加成功"
        } else {
            "小部件已添加到桌面"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
