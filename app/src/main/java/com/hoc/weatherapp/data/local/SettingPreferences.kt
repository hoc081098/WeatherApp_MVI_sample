package com.hoc.weatherapp.data.local

import android.app.Application
import android.content.SharedPreferences
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.utils.delegateVal
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

interface PreferenceInterface<T : Any> {
  val value: T
  val observable: Observable<T>
  /**
   * Save [value] to shared preference
   * @param value
   */
  @Throws(Throwable::class)
  fun save(value: T)
}

/**
 * Base class implement [PreferenceInterface], used to get and save value setting
 */
class BaseSettingPreference<T : Any>(
  sharedPreferences: SharedPreferences,
  getter: SharedPreferences.(key: String, defaultValue: T) -> T,
  defaultValue: T,
  key: String?
) : PreferenceInterface<T> {
  private val _value by sharedPreferences.delegateVal<T>(
    getter = getter,
    defaultValue = defaultValue,
    key = key
  )
  private val subject = BehaviorSubject.createDefault(_value)

  override val value: T = subject.value ?: _value
  override val observable: Observable<T> = subject

  /**
   * We call this method in onPreferenceChange of [androidx.preference.Preference.OnPreferenceChangeListener]
   * It called before the state of the Preference is about to be updated and before the state is persisted.
   * Because value will be persisted by [androidx.preference.Preference], only set [value] to [subject]
   * @param value
   */
  override fun save(value: T) = subject.onNext(value)
}


class SettingPreferences(sharedPreferences: SharedPreferences, androidApplication: Application) {
  val temperatureUnitPreference = BaseSettingPreference(
    getter = { key, defaultValue ->
      getString(key, defaultValue.toString())!!.let { TemperatureUnit.fromString(it) }
    },
    defaultValue = TemperatureUnit.KELVIN,
    key = androidApplication.getString(R.string.key_temperature_unit),
    sharedPreferences = sharedPreferences
  )

  val speedUnitPreference = BaseSettingPreference(
    getter = { key, defaultValue ->
      getString(key, defaultValue.toString())!!.let { SpeedUnit.valueOf(it) }
    },
    defaultValue = SpeedUnit.METERS_PER_SECOND,
    key = androidApplication.getString(R.string.key_speed_unit),
    sharedPreferences = sharedPreferences
  )

  val pressureUnitPreference = BaseSettingPreference(
    getter = { key, defaultValue ->
      getString(key, defaultValue.toString())!!.let { PressureUnit.valueOf(it) }
    },
    defaultValue = PressureUnit.HPA,
    key = androidApplication.getString(R.string.key_pressure_unit),
    sharedPreferences = sharedPreferences
  )


  val showNotificationPreference = BaseSettingPreference(
    key = androidApplication.getString(R.string.key_show_notification),
    defaultValue = true,
    getter = SharedPreferences::getBoolean,
    sharedPreferences = sharedPreferences
  )

  val autoUpdatePreference = BaseSettingPreference(
    key = androidApplication.getString(R.string.key_auto_update),
    defaultValue = true,
    getter = SharedPreferences::getBoolean,
    sharedPreferences = sharedPreferences
  )

  val soundNotificationPreference = BaseSettingPreference(
    key = androidApplication.getString(R.string.key_sound_notification),
    defaultValue = true,
    getter = SharedPreferences::getBoolean,
    sharedPreferences = sharedPreferences
  )
}