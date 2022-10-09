package com.hoc.weatherapp.ui.cities

import com.hoc.weatherapp.data.models.entity.City
import org.threeten.bp.ZonedDateTime

data class CityListItem(
  val city: City,
  val temperatureMin: String,
  val temperatureMax: String,
  val weatherDescription: String,
  val weatherConditionId: Long,
  val weatherIcon: String,
  val isSelected: Boolean = false,
  val lastUpdated: ZonedDateTime
)

fun CityListItem.sameExceptIsSelected(other: CityListItem): Boolean {
  if (this === other) return true

  if (city != other.city) return false
  if (temperatureMin != other.temperatureMin) return false
  if (temperatureMax != other.temperatureMax) return false
  if (weatherDescription != other.weatherDescription) return false
  if (weatherConditionId != other.weatherConditionId) return false
  if (weatherIcon != other.weatherIcon) return false
  if (lastUpdated != other.lastUpdated) return false

  return true
}
