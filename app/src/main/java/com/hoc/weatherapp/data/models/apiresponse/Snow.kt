package com.hoc.weatherapp.data.models.apiresponse

import com.squareup.moshi.Json

class Snow(
  /**
   * Snow volume for last 3 hours
   */
  @Json(name = "3h")
  val _3h: Double? = null
)
