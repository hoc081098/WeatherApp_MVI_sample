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
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.toOptional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.ofType
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
      .toSingle {
        CityAndCurrentWeather().apply {
          this.city = city
          this.currentWeather = weather
        }
      }
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

  override fun refreshCurrentWeather(): Single<CityAndCurrentWeather> {
    return when (val city = sharedPrefUtil.getSelectedCity()) {
      null -> Single.error(NoSelectedCityException)
      else -> weatherApiService
        .getCurrentWeatherByCityId(city.id)
        .subscribeOn(Schedulers.io())
        .flatMap(::saveCityAndCurrentWeather)
    }
  }

  override fun getSelectedCityAndCurrentWeather(): Observable<Optional<CityAndCurrentWeather>> {
    return Observable.merge(
      noneCity,
      city.switchMap {
        currentWeatherLocalDataSource
          .getCityAndCurrentWeatherByCityId(it.id)
          .subscribeOn(Schedulers.io())
          .map { it.toOptional() }
      }
    )
  }

  override fun changeSelectedCity(city: City?): Completable {
    return Completable
      .fromAction { sharedPrefUtil.setSelectedCity(city) }
      .subscribeOn(Schedulers.io())
  }

  override fun addCityByLatLng(latitude: Double, longitude: Double): Single<City> {
    return weatherApiService.getCurrentWeatherByLatLng(latitude, longitude)
      .subscribeOn(Schedulers.io())
      .flatMap(::saveCityAndCurrentWeather)
      .map { it.city }
  }

  override fun deleteCity(city: City): Completable {
    return cityLocalDataSource.deleteCity(city)
      .subscribeOn(Schedulers.io())
      .andThen(
        if (city == sharedPrefUtil.getSelectedCity()) {
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
    return when (val city = sharedPrefUtil.getSelectedCity()) {
      null -> Single.error(NoSelectedCityException)
      else -> weatherApiService
        .get5DayEvery3HourForecastByCityId(city.id)
        .subscribeOn(Schedulers.io())
        .flatMap(::saveFiveDayForecastWeather)
    }
  }
}