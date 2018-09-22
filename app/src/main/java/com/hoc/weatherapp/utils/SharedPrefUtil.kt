package com.hoc.weatherapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.remote.TemperatureUnit
import com.squareup.moshi.Moshi
import java.io.IOException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val i: Int.(Int) -> Unit = {}

class SharedPrefUtil(
    sharedPreferences: SharedPreferences,
    context: Context,
    private val moshi: Moshi
) {
    val temperatureUnit by sharedPreferences.delegate<TemperatureUnit>(
        { key, defaultValue ->
            (getString(key, defaultValue.toString()) ?: defaultValue.toString())
                .let { TemperatureUnit.fromString(it) }
        },
        { key, value -> putString(key, value.toString()) },
        TemperatureUnit.KELVIN,
        context.applicationContext.getString(R.string.key_temperature_unit)
    )
    val showNotification by sharedPreferences.delegate<Boolean>(
        key = context.applicationContext.getString(
            R.string.key_show_notification
        ), default = true
    )
    private var hasSelectedCity by sharedPreferences.delegate<Boolean>()
    private var selectedCityJsonString by sharedPreferences.delegate<String>()

    var selectedCity: City?
        get() = when {
            !hasSelectedCity -> null
            else -> try {
                moshi.adapter(City::class.java).fromJson(selectedCityJsonString)
            } catch (e: IOException) {
                null
            }
        }
        set(value) = when (value) {
            null -> hasSelectedCity = false
            else -> {
                val json = try {
                    moshi.adapter(City::class.java).toJson(value)
                } catch (e: Exception) {
                    null
                }
                if (json == null) {
                    hasSelectedCity = false
                } else {
                    hasSelectedCity = true
                    selectedCityJsonString = json
                }
            }
        }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> SharedPreferences.delegate(
    key: String? = null,
    default: T? = null
): ReadWriteProperty<Any, T> = when (
    val kClass = T::class) {
    Int::class -> delegate(
        SharedPreferences::getInt,
        SharedPreferences.Editor::putInt,
        (default as? Int) ?: 0,
        key
    )
    Long::class -> delegate(
        SharedPreferences::getLong,
        SharedPreferences.Editor::putLong,
        (default as? Long) ?: 0,
        key
    )
    Float::class -> delegate(
        SharedPreferences::getFloat,
        SharedPreferences.Editor::putFloat,
        (default as? Float) ?: 0f,
        key
    )
    Double::class -> delegate(
        SharedPreferences::getDouble,
        SharedPreferences.Editor::putDouble,
        (default as? Double) ?: 0.0,
        key
    )
    Boolean::class -> delegate(
        SharedPreferences::getBoolean,
        SharedPreferences.Editor::putBoolean,
        (default as? Boolean) ?: false,
        key
    )
    String::class -> delegate(
        SharedPreferences::getString,
        SharedPreferences.Editor::putString,
        (default as? String).orEmpty(),
        key
    )
    Set::class -> delegate<Set<String>>(
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

fun <T> SharedPreferences.delegate(
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