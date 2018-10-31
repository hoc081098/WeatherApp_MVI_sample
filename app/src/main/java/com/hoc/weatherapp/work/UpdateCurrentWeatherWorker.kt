package com.hoc.weatherapp.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.showOrUpdateNotification
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateCurrentWeatherWorker(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams), KoinComponent {
  private val repository by inject<Repository>()
  private val sharedPrefUtil by inject<SharedPrefUtil>()

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
        Result.SUCCESS
      },
      onFailure = {
        if (it is NoSelectedCityException) {
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
        }
        Result.FAILURE
      }
    )
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateCurrentWeatherWorker"
    const val TAG = "UpdateCurrentWeatherWorker"
  }
}