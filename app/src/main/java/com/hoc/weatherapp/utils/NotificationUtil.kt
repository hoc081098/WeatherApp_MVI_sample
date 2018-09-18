package com.hoc.weatherapp.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.text.HtmlCompat
import com.hoc.weatherapp.App
import com.hoc.weatherapp.CurrentWeatherFragment
import com.hoc.weatherapp.MainActivity
import com.hoc.weatherapp.data.models.entity.CurrentWeather

const val NOTIFICATION_ID = 2

fun Context.showOrUpdateNotification(weather: CurrentWeather) {
    val text = HtmlCompat.fromHtml(
        "${weather.temperature} ℃<br>${weather.description.capitalize()}<br><i>Update time: ${CurrentWeatherFragment.sdf.format(
            weather.dataTime
        )}</i>",
        HtmlCompat.FROM_HTML_MODE_LEGACY
    )
    val builder = NotificationCompat.Builder(this, App.CHANNEL_ID)
        .setSmallIcon(getIconDrawableFromIconString(weather.icon))
        .setContentTitle("${weather.city.name} - ${weather.city.country}")
        .setContentText("${weather.temperature} ℃")
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setAutoCancel(false)
        .setOngoing(true)
        .setWhen(System.currentTimeMillis())
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val resultPendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(applicationContext, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    builder.setContentIntent(resultPendingIntent)
    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
        NOTIFICATION_ID,
        builder.build()
    )
}

fun Context.cancelNotificationById(id: Int) =
    (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .cancel(id)