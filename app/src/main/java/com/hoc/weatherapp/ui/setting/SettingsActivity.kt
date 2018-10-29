package com.hoc.weatherapp.ui.setting

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
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
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private val repository by inject<Repository>()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences, rootKey)

      findPreference(getString(R.string.key_show_notification)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_temperature_unit)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_pressure_unit)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_speed_unit)).onPreferenceChangeListener = this

      sharedPrefUtil.showNotificationObservable
        .skip(1)
        .switchMap { showNotification ->
          if (showNotification) {
            repository.getSelectedCityAndCurrentWeatherOfSelectedCity()
              .observeOn(AndroidSchedulers.mainThread())
              .map {
                when (it) {
                  is None -> false to None
                  is Some -> true to it
                }
              }
              .onErrorReturn { false to None }
          } else {
            Observable.just(false to None)
          }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(
          onNext = { (showNotification, optional) ->
            val context = requireContext()
            if (showNotification) {
              when (optional) {
                is None -> {
                  view?.snackBar("Please select a city!!")
                  context.cancelNotificationById(WEATHER_NOTIFICATION_ID)
                }
                is Some -> optional.value.run {
                  context.showOrUpdateNotification(
                    cityCountry = city.country,
                    cityName = city.name,
                    weather = currentWeather,
                    unit = sharedPrefUtil.temperatureUnit
                  )
                }
              }
            } else {
              context.cancelNotificationById(WEATHER_NOTIFICATION_ID)
            }
          }
        )
        .addTo(compositeDisposable)
    }

    override fun onDestroyView() {
      super.onDestroyView()
      compositeDisposable.clear()
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
      val key = preference?.key
      when {
        key == getString(R.string.key_show_notification) && newValue is Boolean -> {
          sharedPrefUtil.showNotification = newValue
        }
        key == getString(R.string.key_temperature_unit) && newValue is String -> {
          sharedPrefUtil.temperatureUnit = TemperatureUnit.fromString(newValue)
        }
        key == getString(R.string.key_pressure_unit) && newValue is String -> {
          sharedPrefUtil.pressureUnit = PressureUnit.valueOf(newValue)
        }
        key == getString(R.string.key_speed_unit) && newValue is String -> {
          sharedPrefUtil.speedUnit = SpeedUnit.valueOf(newValue)
        }
      }
      return true
    }
  }
}