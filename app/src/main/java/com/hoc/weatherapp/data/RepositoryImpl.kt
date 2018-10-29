package com.hoc.weatherapp.data

import android.app.Application
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
import com.hoc.weatherapp.work.WorkerUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.ofType
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
  private val sharedPrefUtil: SharedPrefUtil,
  private val application: Application
) : Repository {

  override fun refreshWeatherOf(city: City): Completable {
    return weatherApiService
      .getCurrentWeatherByCityId(city.id)
      .zipWith(weatherApiService.get5DayEvery3HourForecastByCityId(city.id))
      .subscribeOn(Schedulers.io())
      .flatMap {
        saveCityAndCurrentWeather(it.first)
          .zipWith(saveFiveDayForecastWeather(it.second))
      }
      .ignoreElement()
  }

  private val selectedCityProcessor = sharedPrefUtil.selectedCityObservable
  private val noneCity = selectedCityProcessor.ofType<None>()
  private val city = selectedCityProcessor.ofType<Some<City>>().map { it.value }

  override fun getSelectedCity(): Observable<Optional<City>> {
    return selectedCityProcessor
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
    return when (
      val city = sharedPrefUtil.selectedCity) {
      null -> Single.error(NoSelectedCityException)
      else -> weatherApiService
        .getCurrentWeatherByCityId(city.id)
        .subscribeOn(Schedulers.io())
        .flatMap(::saveCityAndCurrentWeather)
    }.doOnSuccess {
      if (sharedPrefUtil.showNotification) {
        application.showOrUpdateNotification(
          weather = it.currentWeather,
          cityCountry = it.city.country,
          unit = sharedPrefUtil.temperatureUnit,
          cityName = it.city.name
        )
      } else {
        application.cancelNotificationById(WEATHER_NOTIFICATION_ID)
      }
    }.doOnError {
      if (it is NoSelectedCityException) {
        application.cancelNotificationById(WEATHER_NOTIFICATION_ID)
        WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
      }
    }
  }

  override fun getSelectedCityAndCurrentWeatherOfSelectedCity(): Observable<Optional<CityAndCurrentWeather>> {
    return Observable.merge(
      noneCity,
      city.switchMap { city ->
        currentWeatherLocalDataSource
          .getCityAndCurrentWeatherByCityId(city.id)
          .subscribeOn(Schedulers.io())
          .map { it.toOptional() }
      }
    )
  }

  override fun changeSelectedCity(city: City?): Completable {
    return Completable
      .fromAction { sharedPrefUtil.selectedCity = city }
      .subscribeOn(Schedulers.io())
  }

  override fun addCityByLatLng(latitude: Double, longitude: Double): Single<City> {
    return weatherApiService.getCurrentWeatherByLatLng(latitude, longitude)
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
    return cityLocalDataSource.deleteCity(city)
      .subscribeOn(Schedulers.io())
      .andThen(
        if (city == sharedPrefUtil.selectedCity) {
          changeSelectedCity(null)
        } else {
          Completable.complete()
        }
      )
  }

  override fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>> {
    return currentWeatherLocalDataSource
      .getAllCityAndCurrentWeathers(querySearch)
      .subscribeOn(Schedulers.io())
  }

  override fun getFiveDayForecastOfSelectedCity(): Observable<Optional<List<DailyWeather>>> {
    return Observable.merge(
      noneCity,
      city.switchMap { city ->
        dailyWeatherLocalDataSource
          .getAllDailyWeathersByCityId(city.id)
          .subscribeOn(Schedulers.io())
          .map { it.toOptional() }
      }
    )
  }

  override fun refreshFiveDayForecastOfSelectedCity(): Single<List<DailyWeather>> {
    return when (
      val city = sharedPrefUtil.selectedCity) {
      null -> Single.error(NoSelectedCityException)
      else -> weatherApiService
        .get5DayEvery3HourForecastByCityId(city.id)
        .subscribeOn(Schedulers.io())
        .flatMap(::saveFiveDayForecastWeather)
    }.doOnError {
      if (it is NoSelectedCityException) {
        application.cancelNotificationById(WEATHER_NOTIFICATION_ID)
        WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
      }
    }
  }
}