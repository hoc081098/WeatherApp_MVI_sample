package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.LocalDataSourceUtil.saveCityAndCurrentWeather
import com.hoc.weatherapp.data.LocalDataSourceUtil.saveFiveDayForecastWeather
import com.hoc.weatherapp.data.local.CityLocalDataSource
import com.hoc.weatherapp.data.local.CurrentWeatherLocalDataSource
import com.hoc.weatherapp.data.local.FiveDayForecastLocalDataSource
import com.hoc.weatherapp.data.local.SelectedCityPreference
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.remote.OpenWeatherMapApiService
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.toOptional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class CityRepositoryImpl(
  private val openWeatherMapApiService: OpenWeatherMapApiService,
  private val cityLocalDataSource: CityLocalDataSource,
  private val fiveDayForecastLocalDataSource: FiveDayForecastLocalDataSource,
  private val currentWeatherLocalDataSource: CurrentWeatherLocalDataSource,
  private val selectedCityPreference: SelectedCityPreference
) : CityRepository {
  /**
   * Get stream of selected city
   * @return [Observable] emit [None] when having no selected city, otherwise emit [Some] of [City]
   */
  override fun getSelectedCity(): Observable<Optional<City>> {
    return selectedCityPreference.observable
  }

  /**
   * Delete [city]
   * @param city
   * @return a [Completable]
   */
  override fun deleteCity(city: City): Single<City> {
    return Completable.mergeArray(
      cityLocalDataSource
        .deleteCity(city)
        .subscribeOn(Schedulers.io()),
      /**
       * If [city] is current selected city, then [changeSelectedCity] to null
       */
      Single
        .fromCallable { selectedCityPreference.value }
        .filter { it.getOrNull() == city }
        .flatMapCompletable { changeSelectedCity(null) }
    ).toSingleDefault(city)
  }

  /**
   * Add city by [latitude] and [longitude]
   * @param latitude
   * @param longitude
   * @return a [Single] emit added city or emit error
   */
  override fun addCityByLatLng(latitude: Double, longitude: Double): Single<City> {
    return openWeatherMapApiService
      .getCurrentWeatherByLatLng(latitude, longitude)
      .subscribeOn(Schedulers.io())
      .flatMap { saveCityAndCurrentWeather(cityLocalDataSource, currentWeatherLocalDataSource, it) }
      .map { it.city }
      .flatMap { city ->
        openWeatherMapApiService
          .get5DayEvery3HourForecastByCityId(city.id)
          .subscribeOn(Schedulers.io())
          .flatMap { saveFiveDayForecastWeather(fiveDayForecastLocalDataSource, it) }
          .map { city }
      }
  }

  /**
   * Change selected city to [city]
   * @param city if [city] is null, indicates that have not selected city
   * @return a [Completable], emit [SaveSelectedCityError] when error
   */
  override fun changeSelectedCity(city: City?): Completable {
    return Completable
      .fromCallable { selectedCityPreference.save(city.toOptional()) }
      .subscribeOn(Schedulers.io())
      .onErrorResumeNext { Completable.error(SaveSelectedCityError(it)) }
  }
}