package com.hoc.weatherapp.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hoc.weatherapp.utils.debug
import java.util.concurrent.TimeUnit

object WorkerUtil {
  private const val TAG = "__WorkerUtil__"

  fun enqueueUpdateCurrentWeatherWorkRequest() {
    debug("enqueueUpdateCurrentWeatherWorkRequest", TAG)

    val updateCurrentWeather = PeriodicWorkRequestBuilder<UpdateCurrentWeatherWorker>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).build()
    WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        updateCurrentWeather
      )
  }

  fun cancelUpdateCurrentWeatherWorkRequest() {
    debug("cancelUpdateCurrentWeatherWorkRequest", TAG)

    WorkManager
      .getInstance()
      .cancelUniqueWork(UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME)
  }

  fun enqueueUpdateDailyWeatherWorkWorkRequest() {
    debug("enqueueUpdateDailyWeatherWorkWorkRequest", TAG)

    val updateDailyWeathers = PeriodicWorkRequestBuilder<UpdateDailyWeatherWork>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).build()

    WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        UpdateDailyWeatherWork.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        updateDailyWeathers
      )
  }

  fun cancelUpdateDailyWeatherWorkWorkRequest() {
    debug("cancelUpdateDailyWeatherWorkWorkRequest", TAG)

    WorkManager
      .getInstance()
      .cancelUniqueWork(UpdateDailyWeatherWork.UNIQUE_WORK_NAME)
  }
}