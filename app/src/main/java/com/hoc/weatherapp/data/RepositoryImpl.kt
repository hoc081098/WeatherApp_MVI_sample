package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.local.CityLocalDataSource
import com.hoc.weatherapp.data.local.CurrentWeatherLocalDataSource
import com.hoc.weatherapp.data.local.DailyWeatherLocalDataSource
import com.hoc.weatherapp.data.models.currentweather.CurrentWeatherResponse
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.data.models.forecastweather.FiveDayForecastResponse
import com.hoc.weatherapp.data.remote.WeatherApiService
import com.hoc.weatherapp.utils.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.rxkotlin.ofType
import io.reactivex.schedulers.Schedulers

private const val TAG = "$#"

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
  override fun getSelectedCity(): Flowable<Optional<City>> {
    return selectedCityProcessor.hide()
  }

  override fun deleteSelectedCity(): Completable {
    return when (val city: Optional<City>? = selectedCityProcessor.value) {
      null -> Completable.error(IllegalStateException("BehaviorProcessor::value is null"))
      is None -> Completable.error(NoSelectedCityException)
      is Some<City> -> deleteCity(city.value)
    }.doOnComplete { debug("::doOnComplete::refreshCurrentWeather", TAG) }
  }

  private fun saveCityAndCurrentWeather(response: CurrentWeatherResponse): Single<CityAndCurrentWeather> {
    val city = Mapper.responseToCity(response)
    val weather = Mapper.responseToCurrentWeatherEntity(response)

    return Completable.mergeArray(
      cityLocalDataSource
        .insertOrUpdateCity(city)
        .doOnComplete {
          debug(
            "::doOnComplete::insertOrUpdateCity $city",
            TAG
          )
        }
        .andThen(
          currentWeatherLocalDataSource
            .insertOrUpdateCurrentWeather(weather)
            .doOnComplete {
              debug(
                "::doOnComplete::deleteWeatherByCityId::insertOrUpdateCurrentWeather $city",
                TAG
              )
            }
        )
        .doOnComplete {
          debug(
            "::doOnComplete::insertOrUpdateCity::deleteWeatherByCityId::insertOrUpdateCurrentWeather $city",
            TAG
          )
        },
      changeSelectedCity(city)
    ).subscribeOn(Schedulers.io()).toSingle {
      CityAndCurrentWeather().apply {
        this.city = city
        this.currentWeather = weather
      }
    }
  }

  private fun saveFiveDayForecaseWeather(response: FiveDayForecastResponse): Completable {
    TODO()
  }

  override fun refreshCurrentWeather(): Single<CityAndCurrentWeather> {
    return when (val city: Optional<City>? = selectedCityProcessor.value) {
      null -> Single.error(IllegalStateException("BehaviorProcessor::value is null"))
      is None -> Single.error(NoSelectedCityException)
      is Some<City> -> weatherApiService.getCurrentWeatherByCityId(city.value.id)
        .subscribeOn(Schedulers.io())
        .flatMap(::saveCityAndCurrentWeather)

    }.doOnSuccess { debug("::doOnSuccess::refreshCurrentWeather", TAG) }
  }

  override fun getCityAndCurrentWeatherByCity(): Flowable<Optional<CityAndCurrentWeather>> {
    return Flowable.merge(
      noneCity,
      city.switchMap {
        currentWeatherLocalDataSource
          .getCityAndCurrentWeatherByCityId(it.id)
          .subscribeOn(Schedulers.io())
          .map { it.toOptional() }
      }
    ).doOnNext { debug("::doOnNext::getCityAndCurrentWeatherByCity $it", TAG) }
  }

  private val selectedCityProcessor =
    BehaviorProcessor.createDefault<Optional<City>>(sharedPrefUtil.selectedCity.toOptional())!!
  private val noneCity = selectedCityProcessor.ofType<None>().doOnNext { debug("NONE", TAG) }
  private val city = selectedCityProcessor.ofType<Some<City>>().map { it.value }
    .doOnNext { debug("CITY: $it", TAG) }

  override fun changeSelectedCity(city: City?): Completable {
    return Completable.fromAction {
      sharedPrefUtil.selectedCity = city
      selectedCityProcessor.onNext(city.toOptional())
    }.subscribeOn(Schedulers.io())
      .doOnComplete { debug("::doOnComplete::changeSelectedCity $city", TAG) }
  }

  override fun getCityInformationByLatLng(latitude: Double, longitude: Double): Single<City> {
    return weatherApiService.getCurrentWeatherByLatLng(latitude, longitude)
      .subscribeOn(Schedulers.io())
      .flatMap(::saveCityAndCurrentWeather)
      .map { it.city }
  }

  override fun deleteCity(city: City): Completable {
    return cityLocalDataSource.deleteCity(city)
      .doOnComplete { debug("::doOnComplete::deleteCity", TAG) }
      .subscribeOn(Schedulers.io())
      .andThen(
        if (city == selectedCityProcessor.value?.getOrNull()) {
          changeSelectedCity(null)
            .doOnComplete { debug("changeSelectedCity to null", TAG) }
        } else {
          Completable.complete()
        }
      )
      .doOnComplete { debug("::doOnComplete::deleteCity::changeSelectedCity", TAG) }
  }

  override fun getAllCityAndCurrentWeathers(querySearch: String): Flowable<List<CityAndCurrentWeather>> {
    return currentWeatherLocalDataSource
      .getAllCityAndCurrentWeathers(querySearch)
      .subscribeOn(Schedulers.io())
  }

  override fun getFiveDayForecastByCity(): Flowable<Optional<List<DailyWeather>>> {
    return Flowable.merge(
      noneCity,
      city.switchMap {
        dailyWeatherLocalDataSource
          .getAllDailyWeathersByCityId(it.id)
          .subscribeOn(Schedulers.io())
          .map { it.toOptional() }
      }
    )
  }

  override fun refreshFiveDayForecase(): Completable {
    return selectedCityProcessor.lastOrError()
      .onErrorReturn { None }
      .flatMapCompletable {
        when (it) {
          is None -> Completable.error(NoSelectedCityException)
          is Some<City> -> weatherApiService.get5DayEvery3HourForecastByCityId(it.value.id)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(::saveFiveDayForecaseWeather)
        }
      }
  }
}