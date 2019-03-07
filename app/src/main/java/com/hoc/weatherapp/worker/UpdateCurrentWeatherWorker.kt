package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.showNotificationIfEnabled
import io.reactivex.Single
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateCurrentWeatherWorker(context: Context, workerParams: WorkerParameters) :
  RxWorker(context, workerParams), KoinComponent {
  private val tag = "__current_worker__"
  private val currentWeatherRepository by inject<CurrentWeatherRepository>()
  private val settingPreferences by inject<SettingPreferences>()

  override fun createWork(): Single<Result> {
    return currentWeatherRepository
      .refreshCurrentWeatherOfSelectedCity()
      .doOnSuccess {
        debug("[SUCCESS] doWork $it", tag)

        applicationContext.showNotificationIfEnabled(it, settingPreferences)
      }
      .doOnError {
        debug("[FAILURE] doWork $it", tag)

        if (it is NoSelectedCityException) {
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
        }
      }
      .map { Result.success(workDataOf("RESULT" to "Update current success")) }
      .onErrorReturn { Result.failure(workDataOf("RESULT" to "Update current failure: ${it.message}")) }
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateCurrentWeatherWorker"
    const val TAG = "UpdateCurrentWeatherWorker"
  }
}