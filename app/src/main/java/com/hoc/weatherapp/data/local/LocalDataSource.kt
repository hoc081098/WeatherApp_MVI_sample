package com.hoc.weatherapp.data.local

import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

class LocalDataSource(
    private val weatherDao: WeatherDao,
    private val dailyWeatherDao: DailyWeatherDao
) {
    fun getCurrentWeatherById(id: Long): Flowable<CurrentWeather> {
        return weatherDao.getCurrentWeatherById(id)
    }

    fun insertOrCurrentWeather(weather: CurrentWeather): Completable {
        return Completable.fromCallable {
            weatherDao.upsert(weather)
        }
    }

    fun getAllWeathers(): Flowable<List<CurrentWeather>> {
        return weatherDao.getAllWeathers()
    }

    fun deleteWeather(weather: CurrentWeather): Completable {
        return Completable.fromAction {
            weatherDao.deleteWeather(weather)
        }
    }

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

    fun getCurrentWeatherByIdSingle(id: Long): Single<CurrentWeather> {
        return weatherDao.getCurrentWeatherByIdSingle(id)
    }

    fun getAllDailyWeathersByCityIdSingle(id: Long): Single<List<DailyWeather>> {
        return dailyWeatherDao.getAllDailyWeathersByCityIdSingle(id)
    }
}