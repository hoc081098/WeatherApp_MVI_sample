package com.hoc.weatherapp.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hoc.weatherapp.App
import com.hoc.weatherapp.MainActivity
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.CurrentWeather

const val NOTIFICATION_ID = 2

fun Context.showOrUpdateNotification(weather: CurrentWeather) {
    val builder = NotificationCompat.Builder(this, App.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle("${weather.city.name} - ${weather.city.country}")
        .setContentText("${weather.main}...${weather.temperature} \u2103")
        .setAutoCancel(false)
        .setOngoing(true)
        .setWhen(weather.dataTime.time)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val resultPendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(applicationContext, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    builder.setContentIntent(resultPendingIntent)
    val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(NOTIFICATION_ID, builder.build())
}