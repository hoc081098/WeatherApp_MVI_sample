package com.hoc.weatherapp.initializer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import androidx.startup.Initializer
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.debug

@Suppress("unused")
class NotificationInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    debug("NotificationInitializer", "Initializer")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        context.getString(R.string.notification_channel_id),
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply { description = "Notification channel of weather app" }

      getSystemService(context, NotificationManager::class.java)!!.createNotificationChannel(channel)
    }
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
