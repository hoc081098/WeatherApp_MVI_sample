package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.currentweather.CurrentWeatherResponse
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.data.models.forecastweather.FiveDayForecastResponse
import java.util.Date

object Mapper {
    @JvmStatic
    fun responseToListDailyWeatherEntity(response: FiveDayForecastResponse): List<DailyWeather> {
        return response.list?.map {
            it.run {
                val firstWeather = weather?.first()
                val city = response.city

                DailyWeather(
                    timeOfDataForecasted = Date((dt ?: 0) * 1_000),
                    cloudiness = clouds?.all ?: 0,
                    description = firstWeather?.description ?: "No description",
                    main = firstWeather?.main ?: "No main weather",
                    groundLevel = main?.grndLevel ?: 0.0,
                    humidity = main?.humidity ?: 0,
                    icon = firstWeather?.icon ?: "",
                    city = City(
                        id = city?.id ?: Long.MIN_VALUE,
                        name = city?.name ?: "",
                        country = city?.country ?: "",
                        lng = city?.coord?.lon ?: Double.NEGATIVE_INFINITY,
                        lat = city?.coord?.lat ?: Double.NEGATIVE_INFINITY
                    ),
                    pressure = main?.pressure ?: 0.0,
                    rainVolumeForTheLast3Hours = rain?._3h ?: 0.0,
                    snowVolumeForTheLast3Hours = snow?._3h ?: 0.0,
                    seaLevel = main?.seaLevel ?: 0.0,
                    temperature = main?.temp ?: 0.0,
                    temperatureMax = main?.tempMax ?: 0.0,
                    temperatureMin = main?.tempMin ?: 0.0,
                    winDegrees = wind?.deg ?: 0.0,
                    winSpeed = wind?.speed ?: 0.0
                )
            }
        } ?: emptyList()
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
    fun responseToCity(response: CurrentWeatherResponse): City {
        return City(
            id = response.id ?: Long.MIN_VALUE,
            name = response.name ?: "",
            country = response.sys?.country ?: "",
            lng = response.coord?.lon ?: Double.NEGATIVE_INFINITY,
            lat = response.coord?.lat ?: Double.NEGATIVE_INFINITY
        )
    }
}