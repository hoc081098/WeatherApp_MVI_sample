package com.hoc.weatherapp.data.models.apiresponse.forecastweatherapiresponse

import com.hoc.weatherapp.data.models.apiresponse.Coord
import com.squareup.moshi.Json

class City(
  /**
   * City ID
   */
  @Json(name = "id")
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
