package com.hoc.weatherapp.data.models.apiresponse.forecastweatherapiresponse

import com.squareup.moshi.Json

class FiveDayForecastResponse(
  /**
   * Internal parameter
   */
  @Json(name = "cod")
  val cod: String? = null,

  /**
   *  Internal parameter
   */
  @Json(name = "message")
  val message: Double? = null,

  /**
   * Number of lines returned by this API call
   */
  @Json(name = "cnt")
  val cnt: Long? = null,

  /**
   * List of daily weathers
   */
  @Json(name = "list")
  val list: List<DailyWeatherModel>? = null,

  /**
   * City information
   */
  @Json(name = "city")
  val city: City? = null
)
