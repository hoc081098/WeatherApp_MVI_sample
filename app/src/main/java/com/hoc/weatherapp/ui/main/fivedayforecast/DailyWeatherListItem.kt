package com.hoc.weatherapp.ui.main.fivedayforecast

import com.hoc.weatherapp.data.models.WindDirection
import java.util.*

interface DailyWeatherListItem {
  data class Weather(
    val weatherIcon: String,
    val dataTime: Date,
    val weatherDescription: String,
    val temperatureMin: String,
    val temperatureMax: String,
    val temperature: String,
    val pressure: String,
    val seaLevel: String,
    val groundLevel: String,
    val humidity: String,
    val main: String,
    val cloudiness: String,
    val winSpeed: String,
    val windDirection: WindDirection,
    val rainVolumeForTheLast3Hours: String,
    val snowVolumeForTheLast3Hours: String
  ) : DailyWeatherListItem

  data class Header(val date: Date) : DailyWeatherListItem
}
