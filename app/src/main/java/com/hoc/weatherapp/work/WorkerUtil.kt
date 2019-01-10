package com.hoc.weatherapp.work

import androidx.work.*
import com.hoc.weatherapp.utils.debug
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object WorkerUtil {
  private fun enqueuePeriodicWorkRequestWithInitialDelay(tag: String) {
    val workRequest = OneTimeWorkRequestBuilder<InitialDelayEnqueueWorker>()
      .setInitialDelay(15, TimeUnit.MINUTES)
      .setInputData(
        workDataOf("TAG" to tag)
      )
      .addTag(tag)
      .build()

    WorkManager
      .getInstance()
      .enqueue(workRequest)
      .result
      .addListener(Runnable{
        debug("")
      }, Executors.newSingleThreadExecutor())
  }

  private fun cancelAllWorkByTag(tag: String) = WorkManager
    .getInstance()
    .cancelAllWorkByTag(tag)

  internal fun enqueueUpdateCurrentWeatherWorkRequestImmediately() {
    val updateCurrentWeather = PeriodicWorkRequestBuilder<UpdateCurrentWeatherWorker>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).addTag(UpdateCurrentWeatherWorker.TAG).build()

    WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        updateCurrentWeather
      )
  }

  internal fun enqueueUpdateDailyWeatherWorkRequestImmediately() {
    val updateDailyWeathers = PeriodicWorkRequestBuilder<UpdateDailyWeatherWork>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).addTag(UpdateDailyWeatherWork.TAG).build()

    WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        UpdateDailyWeatherWork.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        updateDailyWeathers
      )
  }

  fun enqueueUpdateCurrentWeatherWorkRequest() {
    cancelUpdateCurrentWeatherWorkRequest()
    enqueuePeriodicWorkRequestWithInitialDelay(UpdateCurrentWeatherWorker.TAG)
  }

  fun enqueueUpdateDailyWeatherWorkWorkRequest() {
    cancelUpdateDailyWeatherWorkWorkRequest()
    enqueuePeriodicWorkRequestWithInitialDelay(UpdateDailyWeatherWork.TAG)
  }

  fun cancelUpdateCurrentWeatherWorkRequest() = cancelAllWorkByTag(UpdateCurrentWeatherWorker.TAG)

  fun cancelUpdateDailyWeatherWorkWorkRequest() = cancelAllWorkByTag(UpdateDailyWeatherWork.TAG)
}