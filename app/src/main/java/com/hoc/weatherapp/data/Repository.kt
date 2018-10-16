package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.Optional
import io.reactivex.Completable
import io.reactivex.Flowable

interface Repository {
    /**
     * *********************************************************************************************
     * City
     * *********************************************************************************************
     */

    /**
     * if [city] is null, indicates that have not selected city
     */
    fun changeSelectedCity(city: City?): Completable

    fun getCityInformationByLatLng(latitude: Double, longitude: Double): Completable

    fun deleteCity(city: City): Completable

    fun deleteSelectedCity(): Completable

    fun getSelectedCity(): Flowable<Optional<City>>

    /**
     * *********************************************************************************************
     * Current weather
     * *********************************************************************************************
     */

    fun refreshCurrentWeather(): Completable

    fun getCityAndCurrentWeatherByCity(): Flowable<Optional<CityAndCurrentWeather>>

    fun getAllCityAndCurrentWeathers(): Flowable<List<CityAndCurrentWeather>>

    /**
     * *********************************************************************************************
     * Daily forecast weather
     * *********************************************************************************************
     */

    fun getFiveDayForecastByCity(): Flowable<Optional<List<DailyWeather>>>

    fun refreshFiveDayForecase(): Completable
}