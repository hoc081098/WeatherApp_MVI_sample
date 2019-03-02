package com.hoc.weatherapp.ui.setting

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.worker.WorkerUtil.cancelUpdateCurrentWeatherWorkRequest
import com.hoc.weatherapp.worker.WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest
import com.hoc.weatherapp.worker.WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest
import com.hoc.weatherapp.worker.WorkerUtil.enqueueUpdateDailyWeatherWorkWorkRequest
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject

class SettingsActivity : AppCompatActivity() {
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

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences, rootKey)
      /**
       * Set listener
       */
      findPreference(getString(R.string.key_show_notification)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_temperature_unit)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_pressure_unit)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_speed_unit)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_auto_update)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_sound_notification)).run {
        onPreferenceChangeListener = this@SettingFragment
        /**
         * Only show `enable sound notification` when `show notification` is enabled
         */
        settingPreferences.showNotificationPreference.observable
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeBy(onNext = { isVisible = it })
          .addTo(compositeDisposable)
      }

      /**
       * Setup
       */
      setupSettingShowNotificationAndChangeTemperatureUnit()
    }

    private fun setupSettingShowNotificationAndChangeTemperatureUnit() {
      Observable.mergeArray(
        showNotificationS
          .switchMap {
            if (it) {
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

    override fun onDestroyView() {
      super.onDestroyView()
      compositeDisposable.clear()
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
      val key = preference?.key
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
            enqueueUpdateDailyWeatherWorkWorkRequest()
            enqueueUpdateCurrentWeatherWorkRequest()
          } else {
            cancelUpdateCurrentWeatherWorkRequest()
            cancelUpdateDailyWeatherWorkWorkRequest()
          }
        }
        key == getString(R.string.key_sound_notification) && newValue is Boolean -> {
          debug("Sound: $newValue", "SETTINGS")
          settingPreferences.soundNotificationPreference.save(newValue)
        }
      }
      return true
    }
  }
}
