package com.hoc.weatherapp.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.hoc.weatherapp.utils.debug
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Worker util
 * Enqueue and cancel update weather worker
 */
object WorkerUtil {
  /**
   * The executor to run the listener in
   */
  private val backgroundExecutor by lazy { Executors.newSingleThreadExecutor() }

  /**
   * @param [tag] The tag used to identify the work
   * @param [uniqueName] A unique name which for this operation
   * @return A [ListenableFuture] with information about [Operation]
   * [Operation.State.SUCCESS] state.
   */
  private inline fun <reified T : ListenableWorker> enqueuePeriodic(
    tag: String,
    uniqueName: String
  ): ListenableFuture<Operation.State.SUCCESS> {
    val request = PeriodicWorkRequestBuilder<T>(
      repeatInterval = 15,
      repeatIntervalTimeUnit = TimeUnit.MINUTES,
      flexTimeInterval = 15,
      flexTimeIntervalUnit = TimeUnit.MINUTES
    ).addTag(tag).build()

    return WorkManager
      .getInstance()
      .enqueueUniquePeriodicWork(
        uniqueName,
        ExistingPeriodicWorkPolicy.REPLACE,
        request
      )
      .result
  }

  /**
   *
   */
  private inline operator fun <T : Any> ListenableFuture<T>.plus(crossinline listener: () -> Unit) {
    addListener(Runnable { listener() }, backgroundExecutor)
  }

  /**
   * @param [tag] The tag used to identify the work
   * @return A [ListenableFuture] with information about [Operation]
   * [Operation.State.SUCCESS] state.
   */
  @JvmStatic
  private fun cancelAllWorkByTag(tag: String): ListenableFuture<Operation.State.SUCCESS> {
    return WorkManager.getInstance().cancelAllWorkByTag(tag).result
  }

  /**
   * Enqueue update current weather work request
   */
  @JvmStatic fun enqueueUpdateCurrentWeatherWorkRequest() {
    val tag = UpdateCurrentWeatherWorker.TAG

    enqueuePeriodic<UpdateCurrentWeatherWorker>(
      uniqueName = UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME,
      tag = tag
    ) + { debug("[SUCCESS] enqueue current", tag) }
  }

  /**
   * Enqueue update daily weather work request
   */
  @JvmStatic fun enqueueUpdateDailyWeatherWorkRequest() {
    val tag = UpdateDailyWeatherWorker.TAG

    enqueuePeriodic<UpdateDailyWeatherWorker>(
      uniqueName = UpdateDailyWeatherWorker.UNIQUE_WORK_NAME,
      tag = tag
    ) + { debug("[SUCCESS] enqueue daily", tag) }
  }

  /**
   * Cancel update current weather work request
   */
  @JvmStatic fun cancelUpdateCurrentWeatherWorkRequest() {
    val tag = UpdateCurrentWeatherWorker.TAG
    cancelAllWorkByTag(tag) + { debug("[SUCCESS] cancel current", tag) }
  }

  /**
   * Cancel update daily weather work request
   */
  @JvmStatic fun cancelUpdateDailyWeatherWorkRequest() {
    val tag = UpdateDailyWeatherWorker.TAG
    cancelAllWorkByTag(tag) + { debug("[SUCCESS] cancel daily", tag) }
  }
}