package com.hoc.weatherapp.data.models.apiresponse.currentweatherapiresponse

import com.squareup.moshi.Json

class Main(
  @Json(name = "temp")
  val temp: Double? = null,
  @Json(name = "pressure")
  val pressure: Double? = null,
  @Json(name = "humidity")
  val humidity: Long? = null,
  @Json(name = "temp_min")
  val tempMin: Double? = null,
  @Json(name = "temp_max")
  val tempMax: Double? = null
)
