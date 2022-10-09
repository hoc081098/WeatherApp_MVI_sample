package com.hoc.weatherapp.utils

import android.content.SharedPreferences
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
inline fun <reified T> SharedPreferences.delegate(
  key: String? = null,
  default: T? = null
): ReadWriteProperty<Any, T> = when (
  val kClass = T::class
) {
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
    { k, defaultValue -> getStringSet(k, defaultValue)?.toSet() ?: emptySet() },
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

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
      val k = key ?: property.name
      if (!edit().setter(k, value).commit()) {
        throw IllegalStateException("The new value=$value, key=$k were written not successfully ")
      }
    }
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
