package com.hoc.weatherapp.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class InitialDelayEnqueueWorker(context: Context, workerParams: WorkerParameters) :
  Worker(context, workerParams) {
  override fun doWork(): Result {
    return when (inputData.getString("TAG")) {
      UpdateDailyWeatherWork.TAG -> Result.SUCCESS.also { WorkerUtil.enqueueUpdateCurrentWeatherWorkRequestImmediately() }
      UpdateCurrentWeatherWorker.TAG -> Result.SUCCESS.also { WorkerUtil.enqueueUpdateDailyWeatherWorkRequestImmediately() }
      else -> Result.FAILURE
    }
  }
}