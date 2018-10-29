package com.hoc.weatherapp.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hoc.weatherapp.data.Repository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UpdateCurrentWeatherWorker(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams), KoinComponent {
  private val repository by inject<Repository>()

  override fun doWork(): Result {
    return runCatching {
      repository
        .refreshCurrentWeatherOfSelectedCity()
        .blockingGet()
    }.fold(
      onSuccess = { Result.SUCCESS },
      onFailure = { Result.FAILURE }
    )
  }

  companion object {
    const val UNIQUE_WORK_NAME = "UpdateCurrentWeatherWorker"
  }
}