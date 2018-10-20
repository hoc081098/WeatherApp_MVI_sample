package com.hoc.weatherapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.hoc.weatherapp.koin.dataSourceModule
import com.hoc.weatherapp.koin.retrofitModule
import com.hoc.weatherapp.koin.sharePrefUtilModule
import org.koin.android.ext.android.startKoin

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin(this, listOf(retrofitModule, dataSourceModule, sharePrefUtilModule))
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply { description = "Notification channel of weather app" }

      (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .createNotificationChannel(channel)
    }
  }

  companion object {
    const val CHANNEL_ID = "CHANNEL_ID"
    const val CHANNEL_NAME = "CHANNEL_NAME"
  }
}