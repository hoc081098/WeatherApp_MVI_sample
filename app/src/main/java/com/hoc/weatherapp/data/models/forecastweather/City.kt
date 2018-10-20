package com.hoc.weatherapp.data.models.forecastweather

import com.hoc.weatherapp.data.models.Coord
import com.squareup.moshi.Json

class City(
  /**
   * City ID
   */
  @Json(name = "weatherId")
  val id: Long? = null,
  /**
   * City name
   */
  @Json(name = "name")
  val name: String? = null,
  /**
   * Location
   */
  @Json(name = "coord")
  val coord: Coord? = null,
  /**
   * Country code (GB, JP etc.)
   */
  @Json(name = "country")
  val country: String? = null
)