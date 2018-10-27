package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.DailyWeather
import io.reactivex.Completable
import io.reactivex.Observable

class DailyWeatherLocalDataSource(private val dailyWeatherDao: DailyWeatherDao) {
  fun getAllDailyWeathersByCityId(id: Long): Observable<List<DailyWeather>> {
    return dailyWeatherDao.getAllDailyWeathersByCityId(id)
  }

  fun deleteDailyWeathersByCityIdAndInsert(
    cityId: Long,
    weathers: List<DailyWeather>
  ): Completable {
    return Completable.fromAction {
      dailyWeatherDao.deleteDailyWeathersByCityIdAndInsert(cityId, weathers)
    }
  }
}