package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.local.LocalDataSource
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.data.remote.WeatherApiService
import io.reactivex.Flowable
import io.reactivex.Flowable.concatArrayEager

class WeatherRepositoryImpl2(
    private val weatherApiService: WeatherApiService,
    private val localDataSource: LocalDataSource,
    private val weatherRepositoryImpl: WeatherRepositoryImpl
) : WeatherRepository by weatherRepositoryImpl {
    override fun getCurrentWeatherByCity(city: City): Flowable<CurrentWeather> {
        return concatArrayEager(
            localDataSource.getCurrentWeatherByIdSingle(city.id).toFlowable(),
            Flowable.just(isValidLatLng(city.lat, city.lng) to (city.id >= 0))
                .flatMap { (latLngValid, cityIdValid) ->
                    when {
                        cityIdValid -> weatherApiService.getCurrentWeatherByCityId(city.id)
                        latLngValid -> weatherApiService.getCurrentWeatherByLatLng(
                            city.lat,
                            city.lng
                        )
                        else -> weatherApiService.getCurrentWeatherByCityName("${city.name},${city.country}")
                    }
                }
                .map(weatherRepositoryImpl::mapperResponseToCurrentWeatherEntity)
                .flatMap {
                    if (city.id >= 0) {
                        weatherRepositoryImpl.addOrUpdateWeather(it)
                    } else {
                        Flowable.just(it)
                    }
                }
                .materialize()
                .filter { !it.isOnError }
                .dematerialize()
        )
    }

    override fun getFiveDayForecastByCity(city: City): Flowable<List<DailyWeather>> {
        return concatArrayEager(
            localDataSource.getAllDailyWeathersByCityIdSingle(city.id).toFlowable(),
            Flowable.just(isValidLatLng(city.lat, city.lng) to (city.id >= 0))
                .flatMap { (latLngValid, cityIdValid) ->
                    when {
                        cityIdValid -> weatherApiService.get5DayEvery3HourForecastByCityId(city.id)
                        latLngValid -> weatherApiService.get5DayEvery3HourForecastByLatLng(
                            city.lat,
                            city.lng
                        )
                        else -> weatherApiService.get5DayEvery3HourForecastByCityName("${city.name},${city.country}")
                    }
                }
                .map(weatherRepositoryImpl::mapperResponseToListDailyWeatherEntity)
                .flatMap {
                    if (city.id >= 0) {
                        weatherRepositoryImpl.saveDailyWeathersToLocalDatabase(it, city.id)
                    } else {
                        Flowable.just(it)
                    }
                }
                .materialize()
                .filter { !it.isOnError }
                .dematerialize()
        )
    }
}