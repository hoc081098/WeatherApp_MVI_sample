package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import io.reactivex.Single
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateDailyWeatherWorker(context: Context, workerParams: WorkerParameters) :
  RxWorker(context, workerParams), KoinComponent {
  private val tag = "__daily_worker__"
  private val fiveDayForecastRepository by inject<FiveDayForecastRepository>()

  override fun createWork(): Single<Result> {
    return fiveDayForecastRepository
      .refreshFiveDayForecastOfSelectedCity()
      .doOnSuccess {
        debug("[SUCCESS] doWork $it", tag)
      }
      .doOnError {
        if (it is NoSelectedCityException) {
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
        }
        debug("[FAILURE] doWork $it", tag)
      }
      .map { Result.success(workDataOf("RESULT" to "Update daily success")) }
      .onErrorReturn { Result.failure(workDataOf("RESULT" to "Update daily failure: ${it.message}")) }
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateDailyWeatherWorker"
    const val TAG = "UpdateDailyWeatherWorker"
  }
}