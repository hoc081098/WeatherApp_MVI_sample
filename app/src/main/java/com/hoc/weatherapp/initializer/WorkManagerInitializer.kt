package com.hoc.weatherapp.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.worker.UpdateCurrentWeatherWorker
import com.hoc.weatherapp.worker.UpdateDailyWeatherWorker

@Suppress("unused")
class WorkManagerInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    debug("WorkManagerInitializer", "Initializer")

    WorkManager.initialize(
      context,
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.INFO)
        .build()
    )

    WorkManager.getInstance(context).run {
      getWorkInfosForUniqueWorkLiveData(UpdateDailyWeatherWorker.UNIQUE_WORK_NAME)
        .observeForever {
          it.forEach { workInfo ->
            debug(
              "data=${workInfo.outputData.keyValueMap}, info=$workInfo",
              UpdateDailyWeatherWorker.TAG
            )
          }
        }

      getWorkInfosForUniqueWorkLiveData(UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME)
        .observeForever {
          it.forEach { workInfo ->
            debug(
              "data=${workInfo.outputData.keyValueMap}, info=$workInfo",
              UpdateCurrentWeatherWorker.TAG
            )
          }
        }
    }
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
