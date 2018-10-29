package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.Optional
import io.reactivex.Completable
import io.reactivex.Observable
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

  fun getSelectedCity(): Observable<Optional<City>>

  /**
   * *********************************************************************************************
   * Current weather
   * *********************************************************************************************
   */

  fun refreshWeatherOf(city: City): Single<Pair<CityAndCurrentWeather, List<DailyWeather>>>

  fun refreshCurrentWeatherOfSelectedCity(): Single<CityAndCurrentWeather>

  fun getSelectedCityAndCurrentWeatherOfSelectedCity(): Observable<Optional<CityAndCurrentWeather>>

  fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>>

  /**
   * *********************************************************************************************
   * Daily forecast weather
   * *********************************************************************************************
   */

  fun getFiveDayForecastOfSelectedCity(): Observable<Optional<List<DailyWeather>>>

  fun refreshFiveDayForecastOfSelectedCity(): Single<List<DailyWeather>>
}