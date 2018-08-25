package com.hoc.weatherapp.data

import io.reactivex.Completable
import io.reactivex.Flowable

class LocalDataSource(
        private val weatherDao: WeatherDao
) {
    fun getCurrentWeatherById(id: Long): Flowable<Weather> {
        return weatherDao.getCurrentWeatherById(id)
    }

    fun insertOrCurrentWeather(weather: Weather): Completable {
        return Completable.fromCallable {
            weatherDao.upsert(weather)
        }
    }

    fun getAllWeathers(): Flowable<List<Weather>> {
        return weatherDao.getAllWeathers()
    }
}