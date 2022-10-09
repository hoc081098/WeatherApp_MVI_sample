package com.hoc.weatherapp.ui.setting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.ui.BaseAppCompatActivity
import com.hoc.weatherapp.utils.ACTION_CANCEL_NOTIFICATION
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.map
import com.hoc.weatherapp.utils.showOrUpdateNotification
import com.hoc.weatherapp.worker.WorkerUtil.cancelUpdateCurrentWeatherWorkRequest
import com.hoc.weatherapp.worker.WorkerUtil.cancelUpdateDailyWeatherWorkRequest
import com.hoc.weatherapp.worker.WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest
import com.hoc.weatherapp.worker.WorkerUtil.enqueueUpdateDailyWeatherWorkRequest
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.PublishSubject
import kotlin.LazyThreadSafetyMode.NONE
import org.koin.android.ext.android.inject

@ExperimentalStdlibApi
class SettingsActivity : BaseAppCompatActivity(
  contentLayoutId = 0,
  noActionBar = false
) {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      title = "Settings"
    }

    if (supportFragmentManager.findFragmentById(android.R.id.content) === null) {
      supportFragmentManager.beginTransaction()
        .add(android.R.id.content, SettingFragment())
        .commit()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> true.also { finish() }
      else -> super.onOptionsItemSelected(item)
    }
  }

  class SettingFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    private val settingPreferences by inject<SettingPreferences>()
    private val repository by inject<CurrentWeatherRepository>()

    private val showNotificationS = PublishSubject.create<Boolean>()
    private val tempUnitS = PublishSubject.create<TemperatureUnit>()
    private val compositeDisposable = CompositeDisposable()

    private val showNotificationPreference by lazy(NONE) { findPreference<Preference>(getString(R.string.key_show_notification)) as SwitchPreferenceCompat }
    private val broadcastReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        showNotificationPreference.isChecked = false
      }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) =
      setPreferencesFromResource(R.xml.preferences, rootKey)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      /**
       * Set listener
       */

      showNotificationPreference.onPreferenceChangeListener = this
      findPreference<Preference>(getString(R.string.key_temperature_unit))!!.onPreferenceChangeListener =
        this
      findPreference<Preference>(getString(R.string.key_pressure_unit))!!.onPreferenceChangeListener =
        this
      findPreference<Preference>(getString(R.string.key_speed_unit))!!.onPreferenceChangeListener =
        this
      findPreference<Preference>(getString(R.string.key_auto_update))!!.onPreferenceChangeListener =
        this
      findPreference<Preference>(getString(R.string.key_dark_theme))!!.onPreferenceChangeListener =
        this
      findPreference<Preference>(getString(R.string.key_sound_notification))!!.run {
        onPreferenceChangeListener = this@SettingFragment
        /**
         * Only show `enable sound notification` when `show notification` is enabled
         */
        settingPreferences.showNotificationPreference.observable
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeBy(onNext = { isVisible = it })
          .addTo(compositeDisposable)
      }
      findPreference<Preference>("About")!!.setOnPreferenceClickListener {
        Intent(Intent.ACTION_VIEW)
          .apply { data = Uri.parse("https://github.com/hoc081098/WeatherApp.git") }
          .let { startActivity(it); true }
      }

      /**
       * Setup
       */
      setupSettingShowNotificationAndChangeTemperatureUnit()
    }

    private fun setupSettingShowNotificationAndChangeTemperatureUnit() {
      Observable.mergeArray(
        showNotificationS
          .switchMap { showNotification ->
            if (showNotification) {
              repository
                .getSelectedCityAndCurrentWeatherOfSelectedCity()
                .take(1)
                .withLatestFrom(
                  settingPreferences.temperatureUnitPreference.observable,
                  settingPreferences.soundNotificationPreference.observable
                )
                .map { triple -> triple.first.map { Triple(it, triple.second, triple.third) } }
            } else {
              Observable.just(None)
            }
          },
        tempUnitS
          .withLatestFrom(settingPreferences.showNotificationPreference.observable)
          .filter { it.second }
          .switchMap { pair1 ->
            repository
              .getSelectedCityAndCurrentWeatherOfSelectedCity()
              .take(1)
              .withLatestFrom(settingPreferences.soundNotificationPreference.observable)
              .map { pair2 -> pair2.first.map { Triple(it, pair1.first, pair2.second) } }
          }
      ).subscribeBy(onNext = {
        debug("setting $it", "SETTINGS")

        val context = requireContext()
        when (it) {
          is None -> context.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          is Some -> it.value.let { triple ->
            triple.first.run {
              context.showOrUpdateNotification(
                weather = currentWeather,
                city = city,
                unit = triple.second,
                popUpAndSound = triple.third
              )
            }
          }
        }
      }).addTo(compositeDisposable)
    }

    override fun onResume() {
      super.onResume()
      /**
       * Sync state between [com.hoc.weatherapp.CancelNotificationReceiver] and this preference
       */
      showNotificationPreference.isChecked = settingPreferences.showNotificationPreference.value
      LocalBroadcastManager
        .getInstance(requireContext())
        .registerReceiver(broadcastReceiver, IntentFilter(ACTION_CANCEL_NOTIFICATION))
    }

    override fun onPause() {
      super.onPause()

      LocalBroadcastManager
        .getInstance(requireContext())
        .unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
      super.onDestroyView()
      compositeDisposable.clear()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
      val key = preference.key
      when {
        key == getString(R.string.key_show_notification) && newValue is Boolean -> {
          settingPreferences.showNotificationPreference.save(newValue)
          showNotificationS.onNext(newValue)
        }
        key == getString(R.string.key_temperature_unit) && newValue is String -> {
          val temperatureUnit = TemperatureUnit.fromString(newValue)
          settingPreferences.temperatureUnitPreference.save(temperatureUnit)
          tempUnitS.onNext(temperatureUnit)
        }
        key == getString(R.string.key_pressure_unit) && newValue is String -> {
          settingPreferences.pressureUnitPreference.save(PressureUnit.valueOf(newValue))
        }
        key == getString(R.string.key_speed_unit) && newValue is String -> {
          settingPreferences.speedUnitPreference.save(SpeedUnit.valueOf(newValue))
        }
        key == getString(R.string.key_auto_update) && newValue is Boolean -> {
          settingPreferences.autoUpdatePreference.save(newValue)
          if (newValue) {
            enqueueUpdateDailyWeatherWorkRequest()
            enqueueUpdateCurrentWeatherWorkRequest()
          } else {
            cancelUpdateCurrentWeatherWorkRequest()
            cancelUpdateDailyWeatherWorkRequest()
          }
        }
        key == getString(R.string.key_sound_notification) && newValue is Boolean -> {
          debug("Sound: $newValue", "SETTINGS")
          settingPreferences.soundNotificationPreference.save(newValue)
        }
        key == getString(R.string.key_dark_theme) && newValue is Boolean -> {
          settingPreferences.darkThemePreference.save(newValue)
        }
      }
      return true
    }
  }
}
