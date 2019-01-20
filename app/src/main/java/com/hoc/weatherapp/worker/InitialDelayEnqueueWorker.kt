package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class InitialDelayEnqueueWorker(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams) {
  override fun doWork(): Result {
    return when (inputData.getString("TAG")) {
      UpdateDailyWeatherWork.TAG -> Result.success().also { WorkerUtil.enqueueUpdateCurrentWeatherWorkRequestImmediately() }
      UpdateCurrentWeatherWorker.TAG -> Result.success().also { WorkerUtil.enqueueUpdateDailyWeatherWorkRequestImmediately() }
      else -> Result.failure()
    }
  }
}