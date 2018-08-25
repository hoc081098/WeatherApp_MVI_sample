package com.hoc.weatherapp.data

import com.hoc.weatherapp.utils.debug
import io.reactivex.Flowable
import java.util.*

fun isValidLat(lat: Double) = lat in -90.0..90.0

fun isValidLng(lng: Double) = lng in -180.0..180.0

fun isValidLatLng(lat: Double, lng: Double) = isValidLat(lat) && isValidLng(lng)

class WeatherRepositoryImpl(
        private val weatherApiService: WeatherApiService,
        private val localDataSource: LocalDataSource
) : WeatherRepository {
    override fun addOrUpdateWeather(weather: Weather): Flowable<Weather> {
        return localDataSource.insertOrCurrentWeather(weather)
                .andThen(Flowable.just(weather))
    }

    override fun getAllWeathers(): Flowable<List<Weather>> {
        return localDataSource.getAllWeathers()
    }

    override fun getCurrentWeatherByCity(city: City): Flowable<Weather> {
        return Flowable.just(isValidLatLng(city.lat, city.lng))
                .filter { it }
                .flatMap {
                    weatherApiService.getCurrentWeatherByLatLng(city.lat, city.lng).map {
                        it.run {
                            Weather(
                                    city = City(
                                            id = id ?: -1,
                                            name = name ?: "No name",
                                            country = sys?.country ?: "No country",
                                            lng = coord?.lon ?: Double.NEGATIVE_INFINITY,
                                            lat = coord?.lat ?: Double.NEGATIVE_INFINITY
                                    ),
                                    cloudiness = clouds?.all ?: 0,
                                    main = weather?.first()?.main ?: "No main weather",
                                    description = weather?.first()?.description ?: "No description",
                                    icon = weather?.first()?.icon ?: "",
                                    temperature = main?.temp ?: 0.0,
                                    pressure = main?.pressure ?: 0.0,
                                    humidity = main?.humidity ?: 0,
                                    temperatureMin = main?.tempMin ?: 0.0,
                                    temperatureMax = main?.tempMax ?: 0.0,
                                    winSpeed = wind?.speed ?: 0.0,
                                    winDegrees = wind?.deg ?: 0.0,
                                    dataTime = Date((dt ?: 0) * 1_000),
                                    snowVolumeForTheLast3Hours = snow?._3h ?: 0L,
                                    rainVolumeForTheLast3Hours = rain?._3h ?: 0L
                            )
                        }
                    }
                }
                .flatMap(::addOrUpdateWeather)
                .onErrorResumeNext { _: Throwable ->
                    localDataSource.getCurrentWeatherById(city.id)
                }
                .doOnNext { debug(it.toString()) }
                .onBackpressureLatest()
    }
}