package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.local.CityLocalDataSource
import com.hoc.weatherapp.data.local.CurrentWeatherLocalDataSource
import com.hoc.weatherapp.data.local.DailyWeatherLocalDataSource
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.currentweather.CurrentWeatherResponse
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.data.models.forecastweather.FiveDayForecastResponse
import com.hoc.weatherapp.data.remote.WeatherApiService
import com.hoc.weatherapp.utils.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers

object NoSelectedCityException : Exception() {
  override val message = "No selected city"
}

class RepositoryImpl(
  private val weatherApiService: WeatherApiService,
  private val currentWeatherLocalDataSource: CurrentWeatherLocalDataSource,
  private val cityLocalDataSource: CityLocalDataSource,
  private val dailyWeatherLocalDataSource: DailyWeatherLocalDataSource,
  private val sharedPrefUtil: SharedPrefUtil
) : Repository {

  override fun refreshWeatherOf(city: City): Single<Pair<CityAndCurrentWeather, List<DailyWeather>>> {
    return weatherApiService
      .getCurrentWeatherByCityId(city.id)
      .flatMap(::saveCityAndCurrentWeather)
      .zipWith(
        weatherApiService
          .get5DayEvery3HourForecastByCityId(city.id)
          .flatMap(::saveFiveDayForecastWeather)
      )
      .subscribeOn(Schedulers.io())

  }

  override fun getSelectedCity(): Observable<Optional<City>> {
    return sharedPrefUtil.selectedCityObservable
  }

  private fun saveCityAndCurrentWeather(response: CurrentWeatherResponse): Single<CityAndCurrentWeather> {
    val city = Mapper.responseToCity(response)
    val weather = Mapper.responseToCurrentWeatherEntity(response)

    return cityLocalDataSource
      .insertOrUpdateCity(city)
      .andThen(currentWeatherLocalDataSource.insertOrUpdateCurrentWeather(weather))
      .toSingleDefault(
        CityAndCurrentWeather().apply {
          this.city = city
          this.currentWeather = weather
        }
      )
      .doOnSuccess { debug("saveCityAndCurrentWeather successs", "__WorkerUtil__") }
      .subscribeOn(Schedulers.io())
  }

  private fun saveFiveDayForecastWeather(response: FiveDayForecastResponse): Single<List<DailyWeather>> {
    val city = Mapper.responseToCity(response)
    val weathers = Mapper.responseToListDailyWeatherEntity(response)

    return dailyWeatherLocalDataSource
      .deleteDailyWeathersByCityIdAndInsert(weathers = weathers, cityId = city.id)
      .toSingleDefault(weathers)
      .subscribeOn(Schedulers.io())
  }

  override fun refreshCurrentWeatherOfSelectedCity(): Single<CityAndCurrentWeather> {
    return Single.fromCallable { sharedPrefUtil.selectedCity.toOptional() }
      .subscribeOn(Schedulers.io())
      .flatMap {
        when (it) {
          is None -> throw NoSelectedCityException
          is Some -> weatherApiService
            .getCurrentWeatherByCityId(it.value.id)
            .subscribeOn(Schedulers.io())
            .flatMap(::saveCityAndCurrentWeather)
        }
      }
  }

  override fun getSelectedCityAndCurrentWeatherOfSelectedCity(): Observable<Optional<CityAndCurrentWeather>> {
    return sharedPrefUtil
      .selectedCityObservable
      .switchMap { optional ->
        when (optional) {
          is Some -> currentWeatherLocalDataSource
            .getCityAndCurrentWeatherByCityId(optional.value.id)
            .subscribeOn(Schedulers.io())
            .map { it.toOptional() }
          is None -> Observable.just(None)
        }
      }
  }

  override fun changeSelectedCity(city: City?): Completable {
    return Completable
      .fromAction { sharedPrefUtil.selectedCity = city }
      .subscribeOn(Schedulers.io())
  }

  override fun addCityByLatLng(latitude: Double, longitude: Double): Single<City> {
    return weatherApiService
      .getCurrentWeatherByLatLng(latitude, longitude)
      .subscribeOn(Schedulers.io())
      .flatMap(::saveCityAndCurrentWeather)
      .map { it.city }
      .flatMap { city ->
        weatherApiService
          .get5DayEvery3HourForecastByCityId(city.id)
          .subscribeOn(Schedulers.io())
          .flatMap { saveFiveDayForecastWeather(it) }
          .map { city }
      }
  }

  override fun deleteCity(city: City): Completable {
    return Completable.mergeArray(
      cityLocalDataSource.deleteCity(city)
        .subscribeOn(Schedulers.io()),
      Single.fromCallable { sharedPrefUtil.selectedCity.toOptional() }
        .subscribeOn(Schedulers.io())
        .filter { it.getOrNull() == city }
        .flatMapCompletable { changeSelectedCity(null) }
    )
  }

  override fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>> {
    return currentWeatherLocalDataSource
      .getAllCityAndCurrentWeathers(querySearch)
      .subscribeOn(Schedulers.io())
  }

  override fun getFiveDayForecastOfSelectedCity(): Observable<Optional<List<DailyWeather>>> {
    return sharedPrefUtil
      .selectedCityObservable
      .switchMap { optional ->
        when (optional) {
          is Some -> dailyWeatherLocalDataSource
            .getAllDailyWeathersByCityId(optional.value.id)
            .subscribeOn(Schedulers.io())
            .map { it.toOptional() }
          is None -> Observable.just(None)
        }
      }
  }

  override fun refreshFiveDayForecastOfSelectedCity(): Single<List<DailyWeather>> {
    return Single.fromCallable { sharedPrefUtil.selectedCity.toOptional() }
      .subscribeOn(Schedulers.io())
      .flatMap {
        when (it) {
          is None -> throw NoSelectedCityException
          is Some -> weatherApiService
            .get5DayEvery3HourForecastByCityId(it.value.id)
            .subscribeOn(Schedulers.io())
            .flatMap(::saveFiveDayForecastWeather)
        }
      }
  }
}