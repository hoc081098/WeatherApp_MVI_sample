package com.hoc.weatherapp.ui.main.currentweather

data class CurrentWeather(
  val temperatureString: String,
  val pressureString: String,
  val humidity: Long,
  val rainVolumeForThe3HoursMm: Double,
  val weatherConditionId: Long,
  val weatherIcon: String,
  val description: String,
  val dataTimeString: String,
  val zoneId: String,
  /**
   * m/s
   */
  val winSpeed: Double,
  val winSpeedString: String,
  val winDirection: String,
  val visibilityKm: Double
)
