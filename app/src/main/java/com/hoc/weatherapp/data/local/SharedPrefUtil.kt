package com.hoc.weatherapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.toOptional
import com.squareup.moshi.Moshi
import io.reactivex.subjects.BehaviorSubject
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val i: Int.(Int) -> Unit = {}

class SharedPrefUtil(
  sharedPreferences: SharedPreferences,
  context: Context,
  private val moshi: Moshi
) {
  val temperatureUnit by sharedPreferences.delegateVal<TemperatureUnit>(
    { key, defaultValue ->
      getString(key, defaultValue.toString())!!.let { TemperatureUnit.fromString(it) }
    },
    TemperatureUnit.KELVIN,
    context.applicationContext.getString(R.string.key_temperature_unit)
  )
  val showNotification by sharedPreferences.delegate<Boolean>(
    key = context.applicationContext.getString(
      R.string.key_show_notification
    ), default = true
  )
  private var selectedCityJsonString by sharedPreferences.delegate<String>()

  /**
   *************************************************************************************************
   */

  private var _selectedCity: City?
    get() = runCatching {
      moshi.adapter(City::class.java).fromJson(selectedCityJsonString)
    }.getOrNull()
    set(value) {
      runCatching {
        moshi.adapter(City::class.java).toJson(value)
      }.onSuccess {
        selectedCityJsonString = it ?: return@onSuccess
      }
    }

  private val _selectedCitySubject =
    BehaviorSubject.createDefault<Optional<City>>(_selectedCity.toOptional())

  val selectedCityObservable = _selectedCitySubject.hide()!!

  fun setSelectedCity(city: City?) {
    _selectedCity = city
    _selectedCitySubject.onNext(city.toOptional())
  }

  fun getSelectedCity() = _selectedCity
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> SharedPreferences.delegate(
  key: String? = null,
  default: T? = null
): ReadWriteProperty<Any, T> = when (
  val kClass = T::class) {
  Int::class -> delegateVar(
    SharedPreferences::getInt,
    SharedPreferences.Editor::putInt,
    (default as? Int) ?: 0,
    key
  )
  Long::class -> delegateVar(
    SharedPreferences::getLong,
    SharedPreferences.Editor::putLong,
    (default as? Long) ?: 0,
    key
  )
  Float::class -> delegateVar(
    SharedPreferences::getFloat,
    SharedPreferences.Editor::putFloat,
    (default as? Float) ?: 0f,
    key
  )
  Double::class -> delegateVar(
    SharedPreferences::getDouble,
    SharedPreferences.Editor::putDouble,
    (default as? Double) ?: 0.0,
    key
  )
  Boolean::class -> delegateVar(
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean,
    (default as? Boolean) ?: false,
    key
  )
  String::class -> delegateVar(
    SharedPreferences::getString,
    SharedPreferences.Editor::putString,
    (default as? String).orEmpty(),
    key
  )
  Set::class -> delegateVar<Set<String>>(
    SharedPreferences::getStringSet,
    SharedPreferences.Editor::putStringSet,
    (default as? Set<*>).orEmpty().filterIsInstanceTo(mutableSetOf()),
    key
  )
  else -> throw IllegalStateException("Not support for type ${kClass.java.simpleName}")
} as ReadWriteProperty<Any, T>

@PublishedApi
internal fun SharedPreferences.Editor.putDouble(
  key: String,
  value: Double
): SharedPreferences.Editor = putLong(key, value.toRawBits())

@PublishedApi
internal fun SharedPreferences.getDouble(
  key: String,
  defaultValue: Double
): Double = Double.fromBits(getLong(key, defaultValue.toRawBits()))

fun <T> SharedPreferences.delegateVar(
  getter: SharedPreferences.(key: String, defaultValue: T) -> T,
  setter: SharedPreferences.Editor.(key: String, value: T) -> SharedPreferences.Editor,
  defaultValue: T,
  key: String? = null
): ReadWriteProperty<Any, T> {
  return object : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
      getter(key ?: property.name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
      edit().setter(key ?: property.name, value).apply()
  }
}

fun <T> SharedPreferences.delegateVal(
  getter: SharedPreferences.(key: String, defaultValue: T) -> T,
  defaultValue: T,
  key: String? = null
): ReadOnlyProperty<Any, T> {
  return object : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
      getter(key ?: property.name, defaultValue)
  }
}