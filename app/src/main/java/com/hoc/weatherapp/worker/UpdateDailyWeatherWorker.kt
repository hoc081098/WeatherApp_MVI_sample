package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.initializer.startKoinIfNeeded
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.worker.WorkerUtil.cancelUpdateDailyWeatherWorkRequest
import io.reactivex.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.threeten.bp.LocalDateTime

class UpdateDailyWeatherWorker(
  context: Context,
  workerParams: WorkerParameters
) : RxWorker(context, workerParams), KoinComponent {
  private val fiveDayForecastRepository by inject<FiveDayForecastRepository>()

  init {
    applicationContext.startKoinIfNeeded()
  }

  override fun createWork(): Single<Result> {
    return fiveDayForecastRepository
      .refreshFiveDayForecastOfSelectedCity()
      .doOnSubscribe { debug("[RUNNING] doWork ${LocalDateTime.now()}", TAG) }
      .doOnSuccess { debug("[SUCCESS] doWork $it", TAG) }
      .doOnError {
        debug("[FAILURE] doWork $it", TAG)

        if (it is NoSelectedCityException) {
          debug("[FAILURE] cancel work request and notification", TAG)
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          cancelUpdateDailyWeatherWorkRequest()
        }
      }
      .map { Result.success(workDataOf(RESULT to "Update daily success")) }
      .onErrorReturn { Result.failure(workDataOf(RESULT to "Update daily failure: ${it.message}")) }
  }

  companion object {
    const val UNIQUE_WORK_NAME = "com.hoc.weatherapp.worker.UpdateDailyWeatherWorker"
    const val TAG = "com.hoc.weatherapp.worker.UpdateDailyWeatherWorker"
    const val RESULT = "RESULT"
  }
}
