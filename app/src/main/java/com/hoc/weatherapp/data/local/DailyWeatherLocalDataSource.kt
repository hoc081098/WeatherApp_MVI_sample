package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.DailyWeather
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

class DailyWeatherLocalDataSource(private val dailyWeatherDao: DailyWeatherDao) {
    fun getAllDailyWeathersByCityId(id: Long): Flowable<List<DailyWeather>> {
        return dailyWeatherDao.getAllDailyWeathersByCityId(id)
    }

    fun deleteDailyWeathersByCityIdAndInsert(
        id: Long,
        weathers: List<DailyWeather>
    ): Completable {
        return Completable.fromAction {
            dailyWeatherDao.deleteDailyWeathersByCityIdAndInsert(id, weathers)
        }
    }

    fun deleteDailyWeathersByCityId(id: Long): Completable {
        return Completable.fromAction {
            dailyWeatherDao.deleteAllDailyWeathersByCityId(id)
        }
    }

    fun getAllDailyWeathersByCityIdSingle(id: Long): Single<List<DailyWeather>> {
        return dailyWeatherDao.getAllDailyWeathersByCityIdSingle(id)
    }
}