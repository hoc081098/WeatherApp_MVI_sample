package com.hoc.weatherapp.data.remote

import com.hoc.weatherapp.data.models.currentweather.CurrentWeatherResponse
import com.hoc.weatherapp.data.models.forecastweather.FiveDayForecastResponse
import com.hoc.weatherapp.utils.UnitConvertor
import io.reactivex.Flowable
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.DecimalFormat

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

  fun format(temperatureInKelvin: Double): String {
    return NUMBER_FORMAT.format(
      UnitConvertor.convertTemperature(
        temperatureInKelvin,
        this
      )
    ) + symbol()
  }

  fun symbol(): String {
    return when (this) {
      FAHRENHEIT -> "℉"
      CELSIUS -> "℃"
      KELVIN -> "K"
    }
  }

  companion object {
    fun fromString(s: String): TemperatureUnit {
      return when (s.toLowerCase()) {
        "celsius" -> CELSIUS
        "metric" -> CELSIUS
        "kelvin" -> KELVIN
        "" -> KELVIN
        "fahrenheit" -> FAHRENHEIT
        "imperial" -> FAHRENHEIT
        else -> throw IllegalStateException()
      }
    }

    @JvmField
    val NUMBER_FORMAT = DecimalFormat("#.#")
  }
}

interface WeatherApiService {
  @GET("weather")
  fun getCurrentWeatherByLatLng(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double
  ): Single<CurrentWeatherResponse>

  @GET("weather")
  fun getCurrentWeatherByCityId(
    @Query("id") id: Long
  ): Single<CurrentWeatherResponse>

  @GET("weather")
  fun getCurrentWeatherByCityName(
    @Query("q") nameAndCoutryCode: String
  ): Flowable<CurrentWeatherResponse>

  @GET("forecast")
  fun get5DayEvery3HourForecastByLatLng(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double
  ): Flowable<FiveDayForecastResponse>

  @GET("forecast")
  fun get5DayEvery3HourForecastByCityId(
    @Query("id") id: Long
  ): Flowable<FiveDayForecastResponse>

  @GET("forecast")
  fun get5DayEvery3HourForecastByCityName(
    @Query("q") nameAndCountryCode: String
  ): Flowable<FiveDayForecastResponse>
}