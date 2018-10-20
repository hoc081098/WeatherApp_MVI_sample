package com.hoc.weatherapp.data.models

import com.squareup.moshi.Json

class Clouds(
  /**
   * Cloudiness, %
   */
  @Json(name = "all")
  val all: Long? = null
)