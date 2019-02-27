package com.hoc.weatherapp.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import androidx.core.app.NotificationCompat
import androidx.core.text.HtmlCompat
import com.hoc.weatherapp.App
import com.hoc.weatherapp.CancelNotificationReceiver
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.ui.SplashActivity
import com.hoc.weatherapp.utils.ui.getIconDrawableFromCurrentWeather
import java.text.SimpleDateFormat
import java.util.*

const val WEATHER_NOTIFICATION_ID = 2
const val ACTION_CANCEL_NOTIFICATION = "com.hoc.weatherapp.CancelNotificationReceiver"
private val SIMPLE_DATE_FORMAT = SimpleDateFormat("dd/MM/yy HH:mm", Locale.US)

fun Context.showOrUpdateNotification(
  weather: CurrentWeather,
  cityName: String,
  cityCountry: String,
  unit: TemperatureUnit,
  popUpAndSound: Boolean//TODO:something is wrong
) {
  val temperature = unit.format(weather.temperature)

  val text = HtmlCompat.fromHtml(
    """$temperature
      |<br>
      |${weather.description.capitalize()}
      |<br>
      |<i>Update time: ${SIMPLE_DATE_FORMAT.format(weather.dataTime)}</i>
      """.trimMargin(),//TODO: format date
    HtmlCompat.FROM_HTML_MODE_LEGACY
  )
  val builder = NotificationCompat.Builder(this, App.CHANNEL_ID)
    .setSmallIcon(
      getIconDrawableFromCurrentWeather(
        weatherConditionId = weather.weatherConditionId,
        weatherIcon = weather.icon
      )
    )
    .setContentTitle("$cityName - $cityCountry")
    .setContentText(temperature)
    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
    .addAction(
      R.drawable.ic_close,
      "Dismiss",
      PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, CancelNotificationReceiver::class.java).apply {
          action = ACTION_CANCEL_NOTIFICATION
        },
        PendingIntent.FLAG_CANCEL_CURRENT
      )
    )
    .setAutoCancel(false)
    .setOngoing(true)
    .setWhen(System.currentTimeMillis())
    .apply {
      if (popUpAndSound) {
        priority = NotificationCompat.PRIORITY_HIGH
        setDefaults(NotificationCompat.DEFAULT_ALL)
        setSound(RingtoneManager.getDefaultUri(TYPE_NOTIFICATION))
      }
    }

  val resultPendingIntent = PendingIntent.getActivity(
    this,
    0,
    Intent(applicationContext, SplashActivity::class.java),
    PendingIntent.FLAG_UPDATE_CURRENT
  )
  builder.setContentIntent(resultPendingIntent)
  (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
    WEATHER_NOTIFICATION_ID,
    builder.build()
  )

  debug("Show notification", "__showOrUpdateNotification__")
}

fun Context.cancelNotificationById(id: Int) =
  (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    .cancel(id)