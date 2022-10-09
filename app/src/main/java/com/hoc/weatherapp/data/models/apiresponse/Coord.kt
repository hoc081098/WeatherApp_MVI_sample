package com.hoc.weatherapp.data.models.apiresponse

import com.squareup.moshi.Json

class Coord(
  /**
   * City geo location, latitude
   */
  @Json(name = "lon")
  val lon: Double? = null,
  /**
   * City geo location, longitude
   */
  @Json(name = "lat")
  val lat: Double? = null
)
