package com.hoc.weatherapp.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.text.HtmlCompat
import com.hoc.weatherapp.CancelNotificationReceiver
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.ui.SplashActivity
import com.hoc.weatherapp.utils.ui.getIconDrawableFromCurrentWeather
import java.util.*
import org.threeten.bp.format.DateTimeFormatter

const val WEATHER_NOTIFICATION_ID = 2
const val ACTION_CANCEL_NOTIFICATION = "com.hoc.weatherapp.CancelNotificationReceiver"

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yy")
private const val TAG = "__notification__"

@ExperimentalStdlibApi
fun Context.showOrUpdateNotification(
  weather: CurrentWeather,
  city: City,
  unit: TemperatureUnit,
  popUpAndSound: Boolean // TODO:something is wrong
) {
  val temperature = unit.format(weather.temperature)
  val text = HtmlCompat.fromHtml(
    """$temperature
      |<br>
      |${weather.description.capitalize(Locale.ROOT)}
      |<br>
      |<i>Update time: ${weather.dataTime.toZonedDateTime(city.zoneId).format(DATE_TIME_FORMATTER)}</i>
    """.trimMargin(),
    HtmlCompat.FROM_HTML_MODE_LEGACY
  )
  val notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
    .setSmallIcon(
      getIconDrawableFromCurrentWeather(
        weatherConditionId = weather.weatherConditionId,
        weatherIcon = weather.icon
      )
    )
    .setContentTitle("${city.name} - ${city.country}")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
          PendingIntent.FLAG_CANCEL_CURRENT
        }
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

      val resultPendingIntent = PendingIntent.getActivity(
        this@showOrUpdateNotification,
        0,
        Intent(applicationContext, SplashActivity::class.java),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
          PendingIntent.FLAG_CANCEL_CURRENT
        }
      )
      setContentIntent(resultPendingIntent)
    }.build()

  debug(
    "<top>.showOrUpdateNotification weather = [$weather], city = [$city], unit = [$unit], popUpAndSound = [$popUpAndSound]",
    TAG
  )
  debug(
    "<top>.showOrUpdateNotification notification = [$notification]",
    TAG
  )
  (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
    WEATHER_NOTIFICATION_ID,
    notification
  )
}

fun Context.cancelNotificationById(id: Int) =
  (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    .cancel(id).also { debug("<top>.cancelNotificationById id = [$id]", TAG) }

@ExperimentalStdlibApi
fun Context.showNotificationIfEnabled(
  cityAndCurrentWeather: CityAndCurrentWeather,
  settingPreferences: SettingPreferences
) {
  debug("<top>.showNotificationIfEnabled", TAG)
  debug(
    "cityAndCurrentWeather = [$cityAndCurrentWeather], settingPreferences = [$settingPreferences]",
    TAG
  )
  if (settingPreferences.showNotificationPreference.value) {
    showOrUpdateNotification(
      weather = cityAndCurrentWeather.currentWeather,
      city = cityAndCurrentWeather.city,
      unit = settingPreferences.temperatureUnitPreference.value,
      popUpAndSound = settingPreferences.soundNotificationPreference.value
    )
  }
}
