package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.LocalDataSourceUtil.saveCityAndCurrentWeather
import com.hoc.weatherapp.data.LocalDataSourceUtil.saveFiveDayForecastWeather
import com.hoc.weatherapp.data.local.CityLocalDataSource
import com.hoc.weatherapp.data.local.CurrentWeatherLocalDataSource
import com.hoc.weatherapp.data.local.FiveDayForecastLocalDataSource
import com.hoc.weatherapp.data.local.SelectedCityPreference
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.remote.OpenWeatherMapApiService
import com.hoc.weatherapp.data.remote.TimezoneDbApiService
import com.hoc.weatherapp.data.remote.getZoneId
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers

class CityRepositoryImpl(
  private val openWeatherMapApiService: OpenWeatherMapApiService,
  private val timezoneDbApiService: TimezoneDbApiService,
  private val cityLocalDataSource: CityLocalDataSource,
  private val fiveDayForecastLocalDataSource: FiveDayForecastLocalDataSource,
  private val currentWeatherLocalDataSource: CurrentWeatherLocalDataSource,
  private val selectedCityPreference: SelectedCityPreference
) : CityRepository {

  override val selectedCity get() = selectedCityPreference.value.getOrNull()

  override fun getSelectedCity() = selectedCityPreference.observable

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
        .flatMapCompletable { changeSelectedCity(None) }
    ).toSingleDefault(city)
  }

  override fun addCityByLatLng(latitude: Double, longitude: Double): Single<City> {
    return Singles.zip(
      openWeatherMapApiService
        .getCurrentWeatherByLatLng(latitude, longitude)
        .subscribeOn(Schedulers.io()),
      getZoneId(timezoneDbApiService, latitude, longitude)
    )
      .flatMap {
        debug("@@@@@@@$it", "@@@@")
        saveCityAndCurrentWeather(
          cityLocalDataSource,
          currentWeatherLocalDataSource,
          it.first,
          it.second
        )
      }
      .map { it.city }
      .flatMap { city ->
        openWeatherMapApiService
          .get5DayEvery3HourForecastByCityId(city.id)
          .subscribeOn(Schedulers.io())
          .flatMap { saveFiveDayForecastWeather(fiveDayForecastLocalDataSource, it) }
          .map { city }
      }
  }

  override fun changeSelectedCity(city: City) = changeSelectedCity(Some(city))

  private fun changeSelectedCity(optionalCity: Optional<City>): Completable {
    return Completable
      .fromCallable { selectedCityPreference.save(optionalCity) }
      .subscribeOn(Schedulers.single())
      .onErrorResumeNext { Completable.error(SaveSelectedCityError(it)) }
  }
}
