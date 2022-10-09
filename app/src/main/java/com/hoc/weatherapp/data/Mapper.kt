package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.apiresponse.currentweatherapiresponse.CurrentWeatherResponse
import com.hoc.weatherapp.data.models.apiresponse.forecastweatherapiresponse.FiveDayForecastResponse
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.debug
import java.util.*

/**
 * Map response from api to local database entity
 */
object Mapper {
  @JvmStatic
  fun responseToListDailyWeatherEntity(response: FiveDayForecastResponse): List<DailyWeather> {
    return response.list?.map {
      it.run {
        val firstWeather = weather?.first()

        DailyWeather(
          timeOfDataForecasted = Date((dt ?: 0) * 1_000),
          cloudiness = clouds?.all ?: 0,
          description = firstWeather?.description ?: "No description",
          main = firstWeather?.main ?: "No main weather",
          groundLevel = main?.grndLevel ?: 0.0,
          humidity = main?.humidity ?: 0,
          icon = firstWeather?.icon ?: "",
          pressure = main?.pressure ?: 0.0,
          rainVolumeForTheLast3Hours = rain?._3h ?: 0.0,
          snowVolumeForTheLast3Hours = snow?._3h ?: 0.0,
          seaLevel = main?.seaLevel ?: 0.0,
          temperature = main?.temp ?: 0.0,
          temperatureMax = main?.tempMax ?: 0.0,
          temperatureMin = main?.tempMin ?: 0.0,
          winDegrees = wind?.deg ?: 0.0,
          windSpeed = wind?.speed ?: 0.0,
          cityId = response.city?.id ?: -1
        )
      }
    }.orEmpty().onEach { debug(it.cityId, "@#@#") }
  }

  @JvmStatic
  fun responseToCurrentWeatherEntity(response: CurrentWeatherResponse): CurrentWeather {
    return response.run {
      val firstWeather = weather?.first()

      CurrentWeather(
        cityId = id ?: Long.MIN_VALUE,
        cloudiness = clouds?.all ?: 0,
        main = firstWeather?.main ?: "No main weather",
        description = firstWeather?.description
          ?: "No description",
        icon = firstWeather?.icon ?: "",
        temperature = main?.temp ?: 0.0,
        pressure = main?.pressure ?: 0.0,
        humidity = main?.humidity ?: 0,
        temperatureMin = main?.tempMin ?: 0.0,
        temperatureMax = main?.tempMax ?: 0.0,
        winSpeed = wind?.speed ?: 0.0,
        winDegrees = wind?.deg ?: 0.0,
        dataTime = Date((dt ?: 0) * 1_000),
        snowVolumeForThe3Hours = snow?._3h ?: 0.0,
        rainVolumeForThe3Hours = rain?._3h ?: 0.0,
        visibility = visibility ?: 0.0,
        sunrise = Date((sys?.sunrise ?: 0) * 1_000),
        sunset = Date((sys?.sunset ?: 0) * 1_000),
        weatherConditionId = firstWeather?.id ?: -1
      )
    }
  }

  @JvmStatic
  fun responseToCity(response: CurrentWeatherResponse, zoneId: String): City {
    return City(
      id = response.id ?: Long.MIN_VALUE,
      name = response.name ?: "",
      country = response.sys?.country ?: "",
      lng = response.coord?.lon ?: Double.NEGATIVE_INFINITY,
      lat = response.coord?.lat ?: Double.NEGATIVE_INFINITY,
      zoneId = zoneId
    )
  }

  @JvmStatic
  fun responseToCity(response: FiveDayForecastResponse): City {
    val city = response.city
    return City(
      id = city?.id ?: Long.MIN_VALUE,
      name = city?.name ?: "",
      country = city?.country ?: "",
      lng = city?.coord?.lon ?: Double.NEGATIVE_INFINITY,
      lat = city?.coord?.lat ?: Double.NEGATIVE_INFINITY,
      zoneId = "" // not need
    )
  }
}
