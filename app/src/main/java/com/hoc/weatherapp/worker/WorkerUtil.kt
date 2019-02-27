package com.hoc.weatherapp.worker

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
    val updateDailyWeathers = PeriodicWorkRequestBuilder<UpdateDailyWeatherWorker>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).addTag(UpdateDailyWeatherWorker.TAG).build()

    return WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        UpdateDailyWeatherWorker.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        updateDailyWeathers
      )
  }

  @JvmStatic
  fun enqueueUpdateCurrentWeatherWorkRequest() {
    cancelUpdateCurrentWeatherWorkRequest()
      .result
      .addListener(Runnable {
        enqueuePeriodicWorkRequestWithInitialDelay(UpdateCurrentWeatherWorker.TAG)
      }, backgroundExecutor)
  }

  @JvmStatic
  fun enqueueUpdateDailyWeatherWorkWorkRequest() {
    cancelUpdateDailyWeatherWorkWorkRequest()
      .result
      .addListener(Runnable {
        enqueuePeriodicWorkRequestWithInitialDelay(UpdateDailyWeatherWorker.TAG)
      }, backgroundExecutor)
  }

  @JvmStatic
  fun cancelUpdateCurrentWeatherWorkRequest() = cancelAllWorkByTag(UpdateCurrentWeatherWorker.TAG)

  @JvmStatic
  fun cancelUpdateDailyWeatherWorkWorkRequest() = cancelAllWorkByTag(UpdateDailyWeatherWorker.TAG)
}