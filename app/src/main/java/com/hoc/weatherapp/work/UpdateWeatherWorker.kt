package com.hoc.weatherapp.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import com.hoc.weatherapp.App
import com.hoc.weatherapp.MainActivity
import com.hoc.weatherapp.R
import com.hoc.weatherapp.SharedPrefUtil
import com.hoc.weatherapp.data.Weather
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.utils.debug
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateWeatherWorker : Worker(), KoinComponent {
    private val weatherDataSource by inject<WeatherRepository>()
    private val sharePrefUtil by inject<SharedPrefUtil>()

    override fun doWork(): Result {
        val city = sharePrefUtil.selectedCity
        return if (city != null) {
            try {
                val weather = weatherDataSource
                        .getCurrentWeatherByCity(city)
                        .blockingFirst()
                debug("UpdateWeatherWorker::doWork $weather", "MAIN_TAG")
                showNotification(weather)
                Result.SUCCESS
            } catch (e: Exception) {
                Result.FAILURE
            }
        } else {
            Result.FAILURE
        }
    }

    private fun showNotification(weather: Weather) {
        val builder = NotificationCompat.Builder(applicationContext, App.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("${weather.city.name} - ${weather.city.country}")
                .setContentText("${weather.main}...${weather.temperature} \u2103")
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(weather.dataTime.time)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        val resultPendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(resultPendingIntent)
        val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        const val NOTIFICATION_ID = 2
    }
}