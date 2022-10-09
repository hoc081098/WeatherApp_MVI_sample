package com.hoc.weatherapp.data.models.apiresponse

import com.squareup.moshi.Json

class WeatherModel(
  /**
   * Weather condition id
   */
  @Json(name = "id")
  val id: Long? = null,

  /**
   * Group of weather parameters (Rain, Snow, Extreme etc.)
   */
  @Json(name = "main")
  val main: String? = null,

  /**
   * Weather condition within the group
   */
  @Json(name = "description")
  val description: String? = null,

  /**
   * Weather icon id
   */
  @Json(name = "icon")
  val icon: String? = null
)
