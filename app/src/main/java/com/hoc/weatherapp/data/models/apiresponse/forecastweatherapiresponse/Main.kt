package com.hoc.weatherapp.data.models.apiresponse.forecastweatherapiresponse

import com.squareup.moshi.Json

class Main(
  /**
   * Temperature. Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
   */
  @Json(name = "temp")
  val temp: Double? = null,

  /**
   * Minimum temperatureString at the moment of calculation.
   * This is deviation from 'temp' that is possible for large cities and megalopolises geographically expanded (use these parameter optionally).
   * Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
   */
  @Json(name = "temp_min")
  val tempMin: Double? = null,

  /**
   *  Maximum temperatureString at the moment of calculation.
   *  This is deviation from 'temp' that is possible for large cities and megalopolises geographically expanded (use these parameter optionally).
   *  Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
   */
  @Json(name = "temp_max")
  val
  tempMax: Double? = null,

  /**
   * Atmospheric pressure on the sea level by default, hPa
   */
  @Json(name = "pressure")
  val pressure: Double? = null,

  /**
   * Atmospheric pressure on the sea level, hPa
   */
  @Json(name = "sea_level")
  val seaLevel: Double? = null,

  /**
   * Atmospheric pressure on the ground level, hPa
   */
  @Json(name = "grnd_level")
  val grndLevel: Double? = null,

  /**
   * Humidity, %
   */
  @Json(name = "humidity")
  val humidity: Long? = null,

  /**
   * Internal parameter
   */
  @Json(name = "temp_kf")
  val tempKf: Double? = null
)
