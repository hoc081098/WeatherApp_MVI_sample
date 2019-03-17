package com.hoc.weatherapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.utils.ACTION_CANCEL_NOTIFICATION
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class CancelNotificationReceiver : BroadcastReceiver(), KoinComponent {
  private val settingPreferences by inject<SettingPreferences>()

  @SuppressLint("CheckResult")
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == ACTION_CANCEL_NOTIFICATION) {
      val pendingResult = goAsync()
      context.cancelNotificationById(WEATHER_NOTIFICATION_ID)

      Completable
        .fromCallable { settingPreferences.showNotificationPreference.saveActual(false) }
        .subscribeOn(Schedulers.single())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete { settingPreferences.showNotificationPreference.save(false) }
        .doOnTerminate { pendingResult.finish() }
        .subscribeBy(onComplete = {
          LocalBroadcastManager
            .getInstance(context)
            .sendBroadcast(Intent(ACTION_CANCEL_NOTIFICATION))
          debug("[SUCCESS] showNotificationPreference", TAG)
        })
    }
  }

  private companion object {
    const val TAG = "CancelNotificationReceiver"
  }
}
