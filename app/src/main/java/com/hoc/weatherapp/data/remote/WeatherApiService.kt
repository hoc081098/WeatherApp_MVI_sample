package com.hoc.weatherapp.data.remote

import com.hoc.weatherapp.data.models.currentweather.CurrentWeatherResponse
import com.hoc.weatherapp.data.models.forecastweather.FiveDayForecastResponse
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
    ): Flowable<CurrentWeatherResponse>

    @GET("weather")
    fun getCurrentWeatherByCityId(
        @Query("id") id: Long,
        @Query("units") units: TemperatureUnit = TemperatureUnit.CELSIUS,
        @Query("appid") appId: String = APP_ID
    ): Flowable<CurrentWeatherResponse>

    @GET("weather")
    fun getCurrentWeatherByCityName(
        @Query("q") nameAndCoutryCode: String,
        @Query("units") units: TemperatureUnit = TemperatureUnit.CELSIUS,
        @Query("appid") appId: String = APP_ID
    ): Flowable<CurrentWeatherResponse>

    @GET("forecast")
    fun get5DayEvery3HourForecastByLatLng(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: TemperatureUnit = TemperatureUnit.CELSIUS,
        @Query("appid") appId: String = APP_ID
    ): Flowable<FiveDayForecastResponse>

    @GET("forecast")
    fun get5DayEvery3HourForecastByCityId(
        @Query("id") id: Long,
        @Query("units") units: TemperatureUnit = TemperatureUnit.CELSIUS,
        @Query("appid") appId: String = APP_ID
    ): Flowable<FiveDayForecastResponse>

    @GET("forecast")
    fun get5DayEvery3HourForecastByCityName(
        @Query("q") nameAndCountryCode: String,
        @Query("units") units: TemperatureUnit = TemperatureUnit.CELSIUS,
        @Query("appid") appId: String = APP_ID
    ): Flowable<FiveDayForecastResponse>
}