package com.hoc.weatherapp.data.local

import android.app.Application
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.utils.asObservable
import com.hoc.weatherapp.utils.delegate
import com.hoc.weatherapp.utils.delegateVar
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlin.properties.ReadWriteProperty

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
class BaseSettingPreference<T : Any>(delegateProperty: ReadWriteProperty<Any, T>) :
  PreferenceInterface<T> {
  private var _value by delegateProperty
  private val subject = BehaviorSubject.createDefault(_value)

  override val value get() = _value
  override val observable = subject.asObservable()

  /**
   * We call this method in onPreferenceChange of [androidx.preference.Preference.OnPreferenceChangeListener]
   * It called before the state of the Preference is about to be updated and before the state is persisted.
   * Because value will be persisted by [androidx.preference.Preference], only set [value] to [subject]
   * @param value
   */
  @MainThread
  override fun save(value: T) = subject.onNext(value)

  /**
   * Save actual [value] to shared preference, call on worker thread
   * @param value
   */
  @WorkerThread
  fun saveActual(value: T) {
    _value = value
  }
}

class SettingPreferences(sharedPreferences: SharedPreferences, androidApplication: Application) {
  val temperatureUnitPreference = BaseSettingPreference(
    delegateProperty = sharedPreferences.delegateVar(
      getter = { key, defaultValue ->
        getString(key, defaultValue.toString())!!.let { TemperatureUnit.fromString(it) }
      },
      setter = { key, value -> putString(key, value.toString()) },
      defaultValue = TemperatureUnit.KELVIN,
      key = androidApplication.getString(R.string.key_temperature_unit)
    )
  )

  val speedUnitPreference = BaseSettingPreference(
    delegateProperty = sharedPreferences.delegateVar(
      getter = { key, defaultValue ->
        getString(key, defaultValue.toString())!!.let { SpeedUnit.valueOf(it) }
      },
      setter = { key, value -> putString(key, value.toString()) },
      defaultValue = SpeedUnit.METERS_PER_SECOND,
      key = androidApplication.getString(R.string.key_speed_unit)
    )
  )

  val pressureUnitPreference = BaseSettingPreference(
    delegateProperty = sharedPreferences.delegateVar(
      getter = { key, defaultValue ->
        getString(key, defaultValue.toString())!!.let { PressureUnit.valueOf(it) }
      },
      setter = { key, value -> putString(key, value.toString()) },
      defaultValue = PressureUnit.HPA,
      key = androidApplication.getString(R.string.key_pressure_unit)
    )
  )

  val showNotificationPreference = BaseSettingPreference(
    delegateProperty = sharedPreferences.delegate(
      default = true,
      key = androidApplication.getString(R.string.key_show_notification)
    )
  )

  val autoUpdatePreference = BaseSettingPreference(
    delegateProperty = sharedPreferences.delegate(
      default = true,
      key = androidApplication.getString(R.string.key_auto_update)
    )
  )

  val soundNotificationPreference = BaseSettingPreference(
    delegateProperty = sharedPreferences.delegate(
      default = false,
      key = androidApplication.getString(R.string.key_sound_notification)
    )
  )

  val darkThemePreference = BaseSettingPreference(
    delegateProperty = sharedPreferences.delegate(
      default = false,
      key = androidApplication.getString(R.string.key_dark_theme)
    )
  )

  override fun toString() =
    "SettingPreferences($temperatureUnitPreference," +
      " $speedUnitPreference, $pressureUnitPreference," +
      " $showNotificationPreference, $autoUpdatePreference," +
      " $soundNotificationPreference, $darkThemePreference)"
}
