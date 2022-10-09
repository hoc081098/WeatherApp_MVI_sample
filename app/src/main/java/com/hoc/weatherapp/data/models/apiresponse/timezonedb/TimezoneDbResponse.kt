package com.hoc.weatherapp.data.models.apiresponse.timezonedb

import com.squareup.moshi.Json

data class TimezoneDbResponse(
  /**
   * Status of the API query. Either OK or FAILED.
   */
  @Json(name = "status") val status: String,
  /**
   * Error message. Empty if no error.
   */
  @Json(name = "message") val message: String,
  /**
   * The time zone name.
   */
  @Json(name = "zoneName") val zoneName: String
)
