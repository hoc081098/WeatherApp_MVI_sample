package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.Optional
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

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

  fun addCityByLatLng(latitude: Double, longitude: Double): Single<City>

  fun deleteCity(city: City): Completable

  fun deleteSelectedCity(): Completable

  fun getSelectedCity(): Flowable<Optional<City>>

  /**
   * *********************************************************************************************
   * Current weather
   * *********************************************************************************************
   */

  fun refreshCurrentWeather(): Single<CityAndCurrentWeather>

  fun getSelectedCityAndCurrentWeather(): Flowable<Optional<CityAndCurrentWeather>>

  fun getAllCityAndCurrentWeathers(querySearch: String): Flowable<List<CityAndCurrentWeather>>

  /**
   * *********************************************************************************************
   * Daily forecast weather
   * *********************************************************************************************
   */

  fun getFiveDayForecastOfSelectedCity(): Flowable<Optional<List<DailyWeather>>>

  fun refreshFiveDayForecastOfSelectedCity(): Single<List<DailyWeather>>
}