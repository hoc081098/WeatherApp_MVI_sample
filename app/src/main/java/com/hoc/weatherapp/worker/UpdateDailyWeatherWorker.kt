package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateDailyWeatherWorker(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams), KoinComponent {
  private val fiveDayForecastRepository by inject<FiveDayForecastRepository>()

  override fun doWork(): Result {
    return runCatching {
      fiveDayForecastRepository
        .refreshFiveDayForecastOfSelectedCity()
        .blockingGet()
    }.onFailure {
      if (it is NoSelectedCityException) {
        applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
        WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
      }
    }.fold({ Result.success() }, { Result.failure() })
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateDailyWeatherWorker"
    const val TAG = "UpdateDailyWeatherWorker"
  }
}