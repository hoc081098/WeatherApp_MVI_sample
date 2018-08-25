package com.hoc.weatherapp.data

import io.reactivex.Flowable

interface WeatherRepository {

    fun getCurrentWeatherByCity(city: City): Flowable<Weather>

    fun getAllWeathers(): Flowable<List<Weather>>

    fun addOrUpdateWeather(weather: Weather): Flowable<Weather>
}