package com.hoc.weatherapp.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.weatherapp.utils.debug
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object WorkerUtil {
  private const val tag = "__worker__"
  private val backgroundExecutor = Executors.newSingleThreadExecutor()

  /**
   * Actual
   */
  private inline fun <reified T : ListenableWorker> enqueuePeriodic(
    tag: String,
    uniqueName: String
  ): Operation {
    val request = PeriodicWorkRequestBuilder<T>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES
    ).addTag(tag).keepResultsForAtLeast(30, TimeUnit.MINUTES).build()

    return WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        uniqueName,
        ExistingPeriodicWorkPolicy.REPLACE,
        request
      )
  }

  /**
   * Enqueue initial delay one time work request, will trigger periodic work request
   */
  private fun enqueue(tag: String): Operation {
    val workRequest = OneTimeWorkRequestBuilder<InitialDelayEnqueueWorker>()
      .setInitialDelay(15, TimeUnit.MINUTES)
      .setInputData(workDataOf("TAG" to tag))
      .keepResultsForAtLeast(30, TimeUnit.MINUTES)
      .addTag(tag)
      .build()

    return WorkManager
      .getInstance()
      .enqueue(workRequest)
  }

  private fun cancelAllWorkByTag(tag: String): Operation {
    return WorkManager
      .getInstance()
      .cancelAllWorkByTag(tag)
  }

  private class InitialDelayEnqueueWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
      return when (inputData.getString("TAG")) {
        UpdateDailyWeatherWorker.TAG -> {
          Result
            .success()
            .also {
              WorkerUtil.enqueuePeriodic<UpdateCurrentWeatherWorker>(
                UpdateCurrentWeatherWorker.TAG,
                UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME
              ).result.get()
              debug("[SUCCESS] enqueuePeriodic current", tag)
            }
        }
        UpdateCurrentWeatherWorker.TAG -> {
          Result
            .success()
            .also {
              WorkerUtil.enqueuePeriodic<UpdateDailyWeatherWorker>(
                UpdateDailyWeatherWorker.TAG,
                UpdateDailyWeatherWorker.UNIQUE_WORK_NAME
              ).result.get()
              debug("[SUCCESS] enqueuePeriodic daily", tag)
            }
        }
        else -> Result.failure()
      }
    }
  }

  /**
   *
   */

  @JvmStatic
  fun enqueueUpdateCurrentWeatherWorkRequest() {
    cancelUpdateCurrentWeatherWorkRequest()
      .result
      .addListener(Runnable {
        enqueue(UpdateCurrentWeatherWorker.TAG)
          .result
          .addListener(Runnable {
            debug("[SUCCESS] enqueue current", tag)
          }, backgroundExecutor)
      }, backgroundExecutor)
  }

  @JvmStatic
  fun enqueueUpdateDailyWeatherWorkRequest() {
    cancelUpdateDailyWeatherWorkWorkRequest()
      .result
      .addListener(Runnable {
        enqueue(UpdateDailyWeatherWorker.TAG)
          .result
          .addListener(Runnable {
            debug("[SUCCESS] enqueue daily", tag)
          }, backgroundExecutor)
      }, backgroundExecutor)
  }

  @JvmStatic
  fun cancelUpdateCurrentWeatherWorkRequest() = cancelAllWorkByTag(UpdateCurrentWeatherWorker.TAG)

  @JvmStatic
  fun cancelUpdateDailyWeatherWorkWorkRequest() = cancelAllWorkByTag(UpdateDailyWeatherWorker.TAG)
}