package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.local.LocalDataSource
import com.hoc.weatherapp.data.models.currentweather.CurrentWeatherResponse
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.data.models.forecastweather.FiveDayForecastResponse
import com.hoc.weatherapp.data.remote.WeatherApiService
import com.hoc.weatherapp.utils.debug
import io.reactivex.Completable
import io.reactivex.Flowable
import java.util.Date

fun isValidLat(lat: Double) = lat in -90.0..90.0

fun isValidLng(lng: Double) = lng in -180.0..180.0

fun isValidLatLng(lat: Double, lng: Double) = isValidLat(lat) && isValidLng(lng)

class WeatherRepositoryImpl(
    private val weatherApiService: WeatherApiService,
    private val localDataSource: LocalDataSource
) : WeatherRepository {
    override fun getCityInformationAndSaveToLocal(
        latitude: Double,
        longitude: Double
    ): Flowable<City> {
        return weatherApiService.getCurrentWeatherByLatLng(latitude, longitude)
            .map(::mapperResponseToCurrentWeatherEntity)
            .flatMap(::addOrUpdateWeather)
            .map(CurrentWeather::city)
    }

    override fun getFiveDayForecastByCity(city: City): Flowable<List<DailyWeather>> {
        return Flowable.just(isValidLatLng(city.lat, city.lng) to (city.id >= 0))
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
            .map(::mapperResponseToListDailyWeatherEntity)
            .flatMap {
                if (city.id >= 0) {
                    saveDailyWeathersToLocalDatabase(it, city.id)
                } else {
                    Flowable.just(it)
                }
            }
            .onErrorResumeNext { _: Throwable ->
                localDataSource.getAllDailyWeathersByCityId(city.id)
            }
            .doOnNext { debug("getFiveDayForecastByCity ${it.size}") }
            .onBackpressureLatest()
    }

    fun saveDailyWeathersToLocalDatabase(
        dailyWeathers: List<DailyWeather>,
        id: Long
    ): Flowable<List<DailyWeather>> {
        return localDataSource.deleteDailyWeathersByCityIdAndInsert(id, dailyWeathers)
            .andThen(Flowable.just(dailyWeathers))
    }

    override fun deleteCityById(id: Long): Completable {
        return localDataSource.deleteWeatherById(id)
            .andThen(localDataSource.deleteDailyWeathersByCityId(id))
    }

    override fun getAllWeathers(): Flowable<List<CurrentWeather>> {
        return localDataSource.getAllWeathers()
    }

    override fun getCurrentWeatherByCity(city: City): Flowable<CurrentWeather> {
        return Flowable.just(isValidLatLng(city.lat, city.lng) to (city.id >= 0))
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
            .map(::mapperResponseToCurrentWeatherEntity)
            .flatMap {
                if (city.id >= 0) {
                    addOrUpdateWeather(it)
                } else {
                    Flowable.just(it)
                }
            }
            .onErrorResumeNext { _: Throwable ->
                localDataSource.getCurrentWeatherById(city.id)
            }
            .doOnNext { debug(it.toString()) }
            .onBackpressureLatest()
    }

    fun addOrUpdateWeather(weather: CurrentWeather): Flowable<CurrentWeather> {
        return localDataSource.insertOrCurrentWeather(weather)
            .andThen(Flowable.just(weather))
    }

    fun mapperResponseToListDailyWeatherEntity(response: FiveDayForecastResponse): List<DailyWeather> {
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

    fun mapperResponseToCurrentWeatherEntity(response: CurrentWeatherResponse): CurrentWeather {
        return response.run {
            val firstWeather = weather?.first()

            CurrentWeather(
                city = City(
                    id = id ?: Long.MIN_VALUE,
                    name = name ?: "",
                    country = sys?.country ?: "",
                    lng = coord?.lon ?: Double.NEGATIVE_INFINITY,
                    lat = coord?.lat ?: Double.NEGATIVE_INFINITY
                ),
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
                snowVolumeForTheLast3Hours = snow?._3h ?: 0.0,
                rainVolumeForTheLast3Hours = rain?._3h ?: 0.0,
                visibility = visibility ?: 0.0,
                sunrise = Date(sys?.sunrise ?: 0),
                sunset = Date(sys?.sunset ?: 0)
            )
        }
    }
}