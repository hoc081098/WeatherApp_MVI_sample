package com.hoc.weatherapp.ui.main.fivedayforecast

import java.util.Date

interface DailyWeatherListItem {
  data class Weather(
    val weatherIcon: String,
    val dataTime: Date,
    val weatherDescription: String,
    val temperatureMin: String,
    val temperatureMax: String
  ) : DailyWeatherListItem

  data class Header(val date: Date) : DailyWeatherListItem
}
