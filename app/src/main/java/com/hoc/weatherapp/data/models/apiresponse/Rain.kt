package com.hoc.weatherapp.data.models.apiresponse

import com.squareup.moshi.Json

class Rain(
  /**
   * Rain volume for last 3 hours, mm
   */
  @Json(name = "3h")
  val _3h: Double? = null
)
