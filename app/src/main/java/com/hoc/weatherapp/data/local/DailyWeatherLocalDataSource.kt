package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.DailyWeather
import io.reactivex.Completable
import io.reactivex.Flowable

class DailyWeatherLocalDataSource(private val dailyWeatherDao: DailyWeatherDao) {
  fun getAllDailyWeathersByCityId(id: Long): Flowable<List<DailyWeather>> {
    return dailyWeatherDao.getAllDailyWeathersByCityId(id)
  }

  fun deleteDailyWeathersByCityIdAndInsert(cityId: Long, weathers: List<DailyWeather>): Completable {
    return Completable.fromAction {
      dailyWeatherDao.deleteDailyWeathersByCityIdAndInsert(cityId, weathers)
    }
  }

  fun deleteDailyWeathersByCityId(cityId: Long): Completable {
    return Completable.fromAction {
      dailyWeatherDao.deleteAllDailyWeathersByCityId(cityId)
    }
  }
}