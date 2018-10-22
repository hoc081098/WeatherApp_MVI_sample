package com.hoc.weatherapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hoc.weatherapp.utils.ACTION_CANCEL_NOTIFICATION
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById

class CancelNotificationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == ACTION_CANCEL_NOTIFICATION) {
      context.cancelNotificationById(WEATHER_NOTIFICATION_ID)
    }
  }
}
