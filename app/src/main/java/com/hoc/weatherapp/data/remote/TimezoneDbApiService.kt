package com.hoc.weatherapp.data.remote

import com.hoc.weatherapp.data.models.apiresponse.timezonedb.TimezoneDbResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL_TIMEZONE_DB = "http://api.timezonedb.com/v2.1/"
const val TIMEZONE_DB_API_KEY = "AAHLCYFT7MLW"

interface TimezoneDbApiService {
  @GET("get-time-zone")
  fun getTimezoneByLatLng(
    @Query("lat") lat: Double,
    @Query("lng") lng: Double
  ): Single<TimezoneDbResponse>
}