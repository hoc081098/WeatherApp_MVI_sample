package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.City
import io.reactivex.Completable
import io.reactivex.Flowable

class CityLocalDataSource(private val cityDao: CityDao) {
  fun insertCity(city: City): Completable {
    return Completable.fromAction {
      cityDao.insertCity(city)
    }
  }

  fun getCityById(id: Long): Flowable<City> {
    return cityDao.getCityById(id)
  }

  fun updateCity(city: City): Completable {
    return Completable.fromAction {
      cityDao.updateCity(city)
    }
  }

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