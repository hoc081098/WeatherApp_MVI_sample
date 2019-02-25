package com.hoc.weatherapp.ui.main.currentweather

import java.util.*

data class CurrentWeather(
  val temperatureString: String,
  val pressureString: String,
  val humidity: Long,
  val rainVolumeForThe3HoursMm: Double,
  val weatherConditionId: Long,
  val weatherIcon: String,
  val description: String,
  val dataTime: Date,
  /**
   * m/s
   */
  val winSpeed: Double,
  val winSpeedString: String,
  val winDirection: String,
  val visibilityKm: Double
)