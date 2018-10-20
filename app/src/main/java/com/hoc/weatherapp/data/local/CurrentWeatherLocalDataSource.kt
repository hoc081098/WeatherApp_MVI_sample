package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

class CurrentWeatherLocalDataSource(private val currentWeatherDao: CurrentWeatherDao) {
  fun getCityAndCurrentWeatherByCityId(cityId: Long): Flowable<CityAndCurrentWeather> {
    return currentWeatherDao.getCityAndCurrentWeatherByCityId(cityId)
  }

  fun getAllCityAndCurrentWeathers(querySearch: String): Flowable<List<CityAndCurrentWeather>> {
    return currentWeatherDao.getAllCityAndCurrentWeathers(querySearch)
  }

  fun insertOrUpdateCurrentWeather(weather: CurrentWeather): Completable {
    return Completable.fromAction {
      currentWeatherDao.upsert(weather)
    }
  }

  fun getCityAndCurrentWeatherByCityIdAsSingle(id: Long): Single<CityAndCurrentWeather> {
    return currentWeatherDao.getCityAndCurrentWeatherByCityIdAsSingle(id)
  }
}