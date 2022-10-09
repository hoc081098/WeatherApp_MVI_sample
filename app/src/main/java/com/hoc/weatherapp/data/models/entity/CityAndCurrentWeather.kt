package com.hoc.weatherapp.data.models.entity

import androidx.room.Embedded

/**
 * This class captures the relationship between a [City] and a city's [CurrentWeather]s, which is
 * used by Room to fetch the related entities.
 */

class CityAndCurrentWeather {
  @Embedded
  lateinit var city: City

  @Embedded
  lateinit var currentWeather: CurrentWeather

  override fun equals(other: Any?) = when {
    this === other -> true
    javaClass != other?.javaClass -> false
    else -> other is CityAndCurrentWeather && city == other.city && currentWeather == other.currentWeather
  }

  override fun hashCode() = 31 * city.hashCode() + currentWeather.hashCode()
  override fun toString() = "CityAndCurrentWeather(city=$city, currentWeather=$currentWeather)"
}
