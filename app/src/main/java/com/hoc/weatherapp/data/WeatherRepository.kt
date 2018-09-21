package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import io.reactivex.Completable
import io.reactivex.Flowable

interface WeatherRepository {

    fun getCurrentWeatherByCity(city: City): Flowable<CurrentWeather>

    fun getFiveDayForecastByCity(city: City): Flowable<List<DailyWeather>>

    fun getAllWeathers(): Flowable<List<CurrentWeather>>

    fun deleteCityById(id: Long): Completable

    fun getCityInformationAndSaveToLocal(latitude: Double, longitude: Double): Flowable<City>
}