package com.hoc.weatherapp.ui.main.fivedayforecast

import android.os.Parcelable
import com.hoc.weatherapp.data.models.apiresponse.WindDirection
import kotlinx.android.parcel.Parcelize
import java.util.*

interface DailyWeatherListItem {
  @Parcelize
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
  ) : DailyWeatherListItem, Parcelable

  data class Header(val date: Date) : DailyWeatherListItem
}
