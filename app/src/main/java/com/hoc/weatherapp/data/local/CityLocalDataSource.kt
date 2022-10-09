package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.City
import io.reactivex.Completable

/**
 * A wrapper of [CityDao]
 */

class CityLocalDataSource(private val cityDao: CityDao) {
  fun deleteCity(city: City): Completable {
    return Completable.fromAction {
      cityDao.deleteCity(city)
    }
  }

  fun insertOrUpdateCity(city: City): Completable {
    return Completable.fromAction {
      cityDao.upsert(city)
    }
  }
}
