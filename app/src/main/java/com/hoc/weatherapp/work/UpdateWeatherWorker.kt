package com.hoc.weatherapp.work

import android.app.NotificationManager
import android.content.Context
import androidx.work.Worker
import com.hoc.weatherapp.SharedPrefUtil
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.utils.NOTIFICATION_ID
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.showOrUpdateNotification
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.Date

class UpdateWeatherWorker : Worker(), KoinComponent {
    private val weatherDataSource by inject<WeatherRepository>()
    private val sharePrefUtil by inject<SharedPrefUtil>()

    override fun doWork(): Result {
        val city = sharePrefUtil.selectedCity
        return if (city != null) {
            try {
                val weather = weatherDataSource
                        .getCurrentWeatherByCity(city)
                    .blockingLast()
                debug("UpdateWeatherWorker::doWork ${Date()} $weather", "MAIN_TAG")
                applicationContext.showOrUpdateNotification(weather)
                Result.SUCCESS
            } catch (e: Exception) {
                Result.FAILURE
            }
        } else {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            Result.FAILURE
        }
    }
}