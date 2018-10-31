package com.hoc.weatherapp.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateDailyWeatherWork(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams), KoinComponent {
  private val repository by inject<Repository>()

  override fun doWork(): Result {
    return runCatching {
      repository
        .refreshFiveDayForecastOfSelectedCity()
        .blockingGet()
    }.fold(
      onSuccess = { Result.SUCCESS },
      onFailure = {
        if (it is NoSelectedCityException) {
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
        }
        Result.FAILURE
      }
    )
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateDailyWeatherWork"
    const val TAG = "UpdateDailyWeatherWork"
  }
}