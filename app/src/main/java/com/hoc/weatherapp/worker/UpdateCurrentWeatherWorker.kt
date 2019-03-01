package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.showNotificationIfEnabled
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateCurrentWeatherWorker(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams), KoinComponent {
  private val currentWeatherRepository by inject<CurrentWeatherRepository>()
  private val settingPreferences by inject<SettingPreferences>()

  override fun doWork(): Result {
    return runCatching {
      currentWeatherRepository
        .refreshCurrentWeatherOfSelectedCity()
        .blockingGet()

    }.onSuccess {
      applicationContext.showNotificationIfEnabled(it, settingPreferences)
    }.onFailure {
      if (it is NoSelectedCityException) {
        applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
        WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
      }
    }.fold({ Result.success() }, { Result.failure() })
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateCurrentWeatherWorker"
    const val TAG = "UpdateCurrentWeatherWorker"
  }
}