package com.hoc.weatherapp.data.models

import com.hoc.weatherapp.utils.UnitConverter
import java.text.DecimalFormat

private val NUMBER_FORMAT = DecimalFormat("#.#")

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
      UnitConverter.convertTemperature(
        temperatureInKelvin,
        this
      )
    ) + symbol()
  }

  fun symbol(): String {
    return when (this) {
      FAHRENHEIT -> "°F"
      CELSIUS -> "°C"
      KELVIN -> "K"
    }
  }

  companion object {
    fun fromString(s: String): TemperatureUnit {
      return when (s.lowercase()) {
        "celsius" -> CELSIUS
        "metric" -> CELSIUS
        "kelvin" -> KELVIN
        "" -> KELVIN
        "fahrenheit" -> FAHRENHEIT
        "imperial" -> FAHRENHEIT
        else -> throw IllegalStateException()
      }
    }
  }
}

enum class SpeedUnit {
  METERS_PER_SECOND,
  KILOMETERS_PER_HOUR,
  MILES_PER_HOUR;

  fun format(speedInMetersPerSecond: Double): String {
    return NUMBER_FORMAT.format(
      UnitConverter.convertSpeed(
        speedInMetersPerSecond,
        this
      )
    ) + symbol()
  }

  fun symbol(): String {
    return when (this) {
      METERS_PER_SECOND -> "m/s"
      KILOMETERS_PER_HOUR -> "kph"
      MILES_PER_HOUR -> "mph"
    }
  }
}

enum class PressureUnit {
  HPA,
  MM_HG;

  fun format(pressureIn_hPa: Double): String {
    return NUMBER_FORMAT.format(
      UnitConverter.convertPressure(
        pressureIn_hPa,
        this
      )
    ) + symbol()
  }

  fun symbol(): String {
    return when (this) {
      HPA -> "hPa"
      MM_HG -> "mmHg"
    }
  }
}
