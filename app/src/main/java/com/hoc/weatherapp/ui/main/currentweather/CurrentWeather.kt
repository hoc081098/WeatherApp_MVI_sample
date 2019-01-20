package com.hoc.weatherapp.ui.main.currentweather

import java.util.*

data class CurrentWeather(
  val temperature: Double,
  val pressure: Double,
  val humidity: Long,
  val rainVolumeForThe3HoursMm: Double,
  val weatherConditionId: Long,
  val weatherIcon: String,
  val description: String,
  val dataTime: Date,
  val winSpeed: Double,
  val winDegrees: Double,
  val visibilityKm: Double
)