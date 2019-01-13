package com.hoc.weatherapp.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.showOrUpdateNotification
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateCurrentWeatherWorker(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams), KoinComponent {
  private val repository by inject<FiveDayForecastRepository>()
  private val sharedPrefUtil by inject<SettingPreferences>()

  override fun doWork(): Result {
    return runCatching {
      repository
        .refreshCurrentWeatherOfSelectedCity()
        .blockingGet()

    }.fold(
      onSuccess = {
        if (sharedPrefUtil.showNotification) {
          applicationContext.showOrUpdateNotification(
            cityName = it.city.name,
            unit = sharedPrefUtil.temperatureUnit,
            cityCountry = it.city.country,
            weather = it.currentWeather
          )
        }
        Result.success()
      },
      onFailure = {
        if (it is NoSelectedCityException) {
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
        }
        Result.failure()
      }
    )
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateCurrentWeatherWorker"
    const val TAG = "UpdateCurrentWeatherWorker"
  }
}