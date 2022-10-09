package com.hoc.weatherapp.data.models.apiresponse.currentweatherapiresponse

import com.squareup.moshi.Json

class Sys(
  @Json(name = "type")
  val type: Long? = null,
  @Json(name = "weatherId")
  val id: Long? = null,
  @Json(name = "message")
  val message: Double? = null,
  @Json(name = "country")
  val country: String? = null,
  @Json(name = "sunrise")
  val sunrise: Long? = null,
  @Json(name = "sunset")
  val sunset: Long? = null
)
