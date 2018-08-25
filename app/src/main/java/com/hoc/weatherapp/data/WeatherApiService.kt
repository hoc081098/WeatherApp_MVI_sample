package com.hoc.weatherapp.data

import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL = "http://api.openweathermap.org/data/2.5/"
const val APP_ID = "8011ab8a054c3313249763404ac18589"

enum class TemperatureUnit {
    FAHRENHEIT,
    CELSIUS,
    KELVIN;

    override fun toString() = when (this) {
        FAHRENHEIT -> "imperial"
        CELSIUS -> "metric"
        KELVIN -> ""
    }
}

interface WeatherApiService {
    @GET("weather")
    fun getCurrentWeatherByLatLng(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("units") units: TemperatureUnit = TemperatureUnit.CELSIUS,
            @Query("appid") appId: String = APP_ID
    ): Flowable<WeatherResponse>
}