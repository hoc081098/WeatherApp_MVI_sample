package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.initializer.startKoinIfNeeded
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.showNotificationIfEnabled
import com.hoc.weatherapp.worker.WorkerUtil.cancelUpdateCurrentWeatherWorkRequest
import io.reactivex.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.threeten.bp.LocalDateTime

class UpdateCurrentWeatherWorker(
  context: Context,
  workerParams: WorkerParameters
) : RxWorker(context, workerParams), KoinComponent {

  private val currentWeatherRepository by inject<CurrentWeatherRepository>()
  private val settingPreferences by inject<SettingPreferences>()

  init {
    applicationContext.startKoinIfNeeded()
  }

  @ExperimentalStdlibApi
  override fun createWork(): Single<Result> {
    return currentWeatherRepository
      .refreshCurrentWeatherOfSelectedCity()
      .doOnSubscribe { debug("[RUNNING] doWork ${LocalDateTime.now()}", TAG) }
      .doOnSuccess {
        debug("[SUCCESS] doWork $it", TAG)
        applicationContext.showNotificationIfEnabled(it, settingPreferences)
      }
      .doOnError {
        debug("[FAILURE] doWork $it", TAG)

        if (it is NoSelectedCityException) {
          debug("[FAILURE] cancel work request and notification", TAG)
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          cancelUpdateCurrentWeatherWorkRequest()
        }
      }
      .map { Result.success(workDataOf(RESULT to "Update current success")) }
      .onErrorReturn { Result.failure(workDataOf(RESULT to "Update current failure: ${it.message}")) }
  }

  companion object {
    const val UNIQUE_WORK_NAME = "com.hoc.weatherapp.worker.UpdateCurrentWeatherWorker"
    const val TAG = "com.hoc.weatherapp.worker.UpdateCurrentWeatherWorker"
    private const val RESULT = "RESULT"
  }
}
