package com.hoc.weatherapp.work

import androidx.work.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object WorkerUtil {
  private val backgroundExecutor by lazy { Executors.newSingleThreadExecutor() }

  private fun enqueuePeriodicWorkRequestWithInitialDelay(tag: String): Operation {
    val workRequest = OneTimeWorkRequestBuilder<InitialDelayEnqueueWorker>()
      .setInitialDelay(15, TimeUnit.MINUTES)
      .setInputData(workDataOf("TAG" to tag))
      .addTag(tag)
      .build()

    return WorkManager
      .getInstance()
      .enqueue(workRequest)
  }

  private fun cancelAllWorkByTag(tag: String): Operation = WorkManager
    .getInstance()
    .cancelAllWorkByTag(tag)

  internal fun enqueueUpdateCurrentWeatherWorkRequestImmediately(): Operation {
    val updateCurrentWeather = PeriodicWorkRequestBuilder<UpdateCurrentWeatherWorker>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).addTag(UpdateCurrentWeatherWorker.TAG).build()

    return WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        updateCurrentWeather
      )
  }

  internal fun enqueueUpdateDailyWeatherWorkRequestImmediately(): Operation {
    val updateDailyWeathers = PeriodicWorkRequestBuilder<UpdateDailyWeatherWork>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).addTag(UpdateDailyWeatherWork.TAG).build()

    return WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        UpdateDailyWeatherWork.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        updateDailyWeathers
      )
  }

  fun enqueueUpdateCurrentWeatherWorkRequest() {
    cancelUpdateCurrentWeatherWorkRequest()
      .result
      .addListener(Runnable {
        enqueuePeriodicWorkRequestWithInitialDelay(UpdateCurrentWeatherWorker.TAG)
      }, backgroundExecutor)
  }

  fun enqueueUpdateDailyWeatherWorkWorkRequest() {
    cancelUpdateDailyWeatherWorkWorkRequest()
      .result
      .addListener(Runnable {
        enqueuePeriodicWorkRequestWithInitialDelay(UpdateDailyWeatherWork.TAG)
      }, backgroundExecutor)
  }

  fun cancelUpdateCurrentWeatherWorkRequest() = cancelAllWorkByTag(UpdateCurrentWeatherWorker.TAG)

  fun cancelUpdateDailyWeatherWorkWorkRequest() = cancelAllWorkByTag(UpdateDailyWeatherWork.TAG)
}