package com.hoc.weatherapp.data.models

import com.hoc.weatherapp.utils.UnitConvertor
import java.text.DecimalFormat

enum class TemperatureUnit {
  FAHRENHEIT,
  CELSIUS,
  KELVIN;

  override fun toString() = when (this) {
    FAHRENHEIT -> "imperial"
    CELSIUS -> "metric"
    KELVIN -> ""
  }

  fun format(temperatureInKelvin: Double): String {
    return NUMBER_FORMAT.format(
      UnitConvertor.convertTemperature(
        temperatureInKelvin,
        this
      )
    ) + symbol()
  }

  fun symbol(): String {
    return when (this) {
      FAHRENHEIT -> "℉"
      CELSIUS -> "℃"
      KELVIN -> "K"
    }
  }

  companion object {
    fun fromString(s: String): TemperatureUnit {
      return when (s.toLowerCase()) {
        "celsius" -> CELSIUS
        "metric" -> CELSIUS
        "kelvin" -> KELVIN
        "" -> KELVIN
        "fahrenheit" -> FAHRENHEIT
        "imperial" -> FAHRENHEIT
        else -> throw IllegalStateException()
      }
    }

    @JvmField
    val NUMBER_FORMAT = DecimalFormat("#.#")
  }
}