package com.hoc.weatherapp.work

import androidx.work.Worker
import com.hoc.weatherapp.SharedPrefUtil
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.utils.NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.showOrUpdateNotification
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.Date

class UpdateCurrentWeatherWorker : Worker(), KoinComponent {
    private val weatherRepository by inject<WeatherRepository>()
    private val sharePrefUtil by inject<SharedPrefUtil>()

    override fun doWork(): Result {
        debug("UpdateCurrentWeatherWorker::doWork time=${Date()}")

        val city = sharePrefUtil.selectedCity
        return if (city != null) {
            try {
                val currentWeather = weatherRepository
                    .getCurrentWeatherByCity(city)
                    .blockingLast()

                debug("UpdateCurrentWeatherWorker::doWork currentWeather=$currentWeather")

                applicationContext.showOrUpdateNotification(currentWeather)
                Result.SUCCESS
            } catch (e: Exception) {
                debug("UpdateCurrentWeatherWorker::doWork e=$e")

                Result.FAILURE
            }
        } else {
            debug("UpdateCurrentWeatherWorker::doWork no selected city")

            applicationContext.cancelNotificationById(NOTIFICATION_ID)
            Result.FAILURE
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "UpdateCurrentWeatherWorker"
    }
}