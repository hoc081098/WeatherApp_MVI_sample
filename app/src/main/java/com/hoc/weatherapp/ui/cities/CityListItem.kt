package com.hoc.weatherapp.ui.cities

import com.hoc.weatherapp.data.models.entity.City

data class CityListItem(
  val city: City,
  val temperatureMin: String,
  val temperatureMax: String,
  val weatherDescription: String,
  val weatherConditionId: Long,
  val weatherIcon: String,
  val isSelected: Boolean = false
)