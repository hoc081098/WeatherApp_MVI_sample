package com.hoc.weatherapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.WorkManager
import com.hoc.weatherapp.koin.dataSourceModule
import com.hoc.weatherapp.koin.presenterModule
import com.hoc.weatherapp.koin.retrofitModule
import com.hoc.weatherapp.koin.sharePrefUtilModule
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.worker.UpdateCurrentWeatherWorker
import com.hoc.weatherapp.worker.UpdateDailyWeatherWorker
import com.jakewharton.threetenabp.AndroidThreeTen
import io.reactivex.plugins.RxJavaPlugins
import org.koin.android.ext.android.startKoin

class App : Application() {
  override fun onCreate() {
    super.onCreate()

    AndroidThreeTen.init(this)

    RxJavaPlugins.setErrorHandler { debug("RxJava error: $it", "RXJAVA_ERROR", it) }

    startKoin(
      this,
      listOf(
        retrofitModule,
        dataSourceModule,
        sharePrefUtilModule,
        presenterModule
      )
    )

    createNotificationChannel()

    observeWorkInfo()
  }

  private fun observeWorkInfo() {
    WorkManager.getInstance().run {
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

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply { description = "Notification channel of weather app" }

      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  companion object {
    const val CHANNEL_ID = "com.hoc.weatherapp"
    const val CHANNEL_NAME = "Weather app"
  }
}