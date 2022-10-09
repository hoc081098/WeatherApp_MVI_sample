package com.hoc.weatherapp.data.models.apiresponse

import com.squareup.moshi.Json

class Wind(
  /**
   * Wind speed. Unit Default: meter/sec, Metric: meter/sec, Imperial: miles/hour.
   */
  @Json(name = "speed")
  val speed: Double? = null,

  /**
   *  Wind direction, degrees (meteorological)
   */
  @Json(name = "deg")
  val deg: Double? = null
)
