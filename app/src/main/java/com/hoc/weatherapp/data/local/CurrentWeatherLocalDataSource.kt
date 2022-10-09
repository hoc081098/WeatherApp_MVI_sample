package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import io.reactivex.Completable
import io.reactivex.Observable

/**
 * A wrapper of [CurrentWeatherDao]
 */

class CurrentWeatherLocalDataSource(private val currentWeatherDao: CurrentWeatherDao) {
  fun getCityAndCurrentWeatherByCityId(cityId: Long): Observable<CityAndCurrentWeather> {
    return currentWeatherDao
      .getCityAndCurrentWeatherByCityId(cityId)
      .distinctUntilChanged()
  }

  fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>> {
    return currentWeatherDao
      .getAllCityAndCurrentWeathers(querySearch)
      .distinctUntilChanged()
  }

  fun insertOrUpdateCurrentWeather(weather: CurrentWeather): Completable {
    return Completable.fromAction {
      currentWeatherDao.upsert(weather)
    }
  }
}
