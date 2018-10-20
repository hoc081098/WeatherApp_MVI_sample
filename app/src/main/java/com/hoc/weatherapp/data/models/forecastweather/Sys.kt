package com.hoc.weatherapp.data.models.forecastweather

import com.squareup.moshi.Json

class Sys(
  @Json(name = "pod")
  val pod: String? = null
)