package com.hoc.weatherapp.work

import androidx.work.Worker
import com.hoc.weatherapp.SharedPrefUtil
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.utils.NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.Date

class UpdateDailyWeatherWork : Worker(), KoinComponent {
    private val weatherRepository by inject<WeatherRepository>()
    private val sharePrefUtil by inject<SharedPrefUtil>()

    override fun doWork(): Result {
        debug("UpdateDailyWeatherWork::doWork time=${Date()}")

        val city = sharePrefUtil.selectedCity
        return if (city != null) {
            try {
                val dailyWeathers = weatherRepository
                    .getFiveDayForecastByCity(city)
                    .blockingLast()

                debug("UpdateDailyWeatherWork::doWork dailyWeathers=$dailyWeathers")

                Result.SUCCESS
            } catch (e: Exception) {
                debug("UpdateDailyWeatherWork::doWork e=$e")

                Result.FAILURE
            }
        } else {
            debug("UpdateDailyWeatherWork::doWork no selected city")

            applicationContext.cancelNotificationById(NOTIFICATION_ID)
            Result.FAILURE
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "UpdateDailyWeatherWork"
    }
}