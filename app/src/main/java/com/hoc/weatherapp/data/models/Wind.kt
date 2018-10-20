package com.hoc.weatherapp.data.models

import com.squareup.moshi.Json

enum class WindDirection {
  N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW;

  companion object {
    fun fromDegrees(deg: Double): WindDirection {
      val d = (deg / 22.5 + 0.5).toInt() % 16
      return WindDirection.values()[d]
    }
  }
}

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
