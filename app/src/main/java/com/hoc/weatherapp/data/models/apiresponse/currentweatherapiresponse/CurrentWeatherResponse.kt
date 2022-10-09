package com.hoc.weatherapp.data.models.apiresponse.currentweatherapiresponse

import com.hoc.weatherapp.data.models.apiresponse.Clouds
import com.hoc.weatherapp.data.models.apiresponse.Coord
import com.hoc.weatherapp.data.models.apiresponse.Rain
import com.hoc.weatherapp.data.models.apiresponse.Snow
import com.hoc.weatherapp.data.models.apiresponse.WeatherModel
import com.hoc.weatherapp.data.models.apiresponse.Wind
import com.squareup.moshi.Json

class CurrentWeatherResponse(
  @Json(name = "coord")
  val coord: Coord? = null,
  @Json(name = "weather")
  val weather: List<WeatherModel>? = null,
  @Json(name = "base")
  val base: String? = null,
  @Json(name = "main")
  val main: Main? = null,
  @Json(name = "visibilityKm")
  val visibility: Double? = null,
  @Json(name = "wind")
  val wind: Wind? = null,
  @Json(name = "clouds")
  val clouds: Clouds? = null,
  @Json(name = "dt")
  val dt: Long? = null,
  @Json(name = "sys")
  val sys: Sys? = null,
  @Json(name = "id")
  val id: Long? = null,
  @Json(name = "name")
  val name: String? = null,
  @Json(name = "cod")
  val cod: Long? = null,
  @Json(name = "rain")
  val rain: Rain? = null,
  @Json(name = "snow")
  val snow: Snow? = null
)
