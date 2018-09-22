package com.hoc.weatherapp.utils

import com.hoc.weatherapp.data.remote.TemperatureUnit

object UnitConvertor {
    /**
     * Convert temperatureInKelvin from KELVIN to @param to
     */
    fun convertTemperature(temperatureInKelvin: Double, to: TemperatureUnit): Double {
        return when (to) {
            TemperatureUnit.KELVIN -> temperatureInKelvin
            TemperatureUnit.CELSIUS -> kelvinToCelsius(temperatureInKelvin)
            TemperatureUnit.FAHRENHEIT -> kelvinToFahrenheit(temperatureInKelvin)
        }
    }

    private fun kelvinToFahrenheit(temperature: Double): Double {
        return temperature * 9.0 / 5 - 459.67
    }

    private fun kelvinToCelsius(temperature: Double): Double {
        return temperature - 272.15
    }
}