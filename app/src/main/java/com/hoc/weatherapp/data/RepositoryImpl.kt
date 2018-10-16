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
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.SharedPrefUtil
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.toOptional
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.rxkotlin.ofType
import io.reactivex.schedulers.Schedulers

private const val TAG = "$#"

object NoSelectedCity : Exception()

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
            is None -> Completable.error(NoSelectedCity)
            is Some<City> -> deleteCity(city.value)
        }.doOnComplete { debug("::doOnComplete::refreshCurrentWeather", TAG) }
    }

    private fun saveCityAndCurrentWeather(response: CurrentWeatherResponse): Completable {
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
        ).subscribeOn(Schedulers.io())
    }

    private fun saveFiveDayForecaseWeather(response: FiveDayForecastResponse): Completable {
        TODO()
    }

    override fun refreshCurrentWeather(): Completable {
        return when (val city: Optional<City>? = selectedCityProcessor.value) {
            null -> Completable.error(IllegalStateException("BehaviorProcessor::value is null"))
            is None -> Completable.error(NoSelectedCity)
            is Some<City> -> weatherApiService.getCurrentWeatherByCityId(city.value.id)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(::saveCityAndCurrentWeather)
        }.doOnComplete { debug("::doOnComplete::refreshCurrentWeather", TAG) }
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

    override fun getCityInformationByLatLng(latitude: Double, longitude: Double): Completable {
        return weatherApiService.getCurrentWeatherByLatLng(latitude, longitude)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(::saveCityAndCurrentWeather)
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

    override fun getAllCityAndCurrentWeathers(): Flowable<List<CityAndCurrentWeather>> {
        return currentWeatherLocalDataSource
            .getAllCityAndCurrentWeathers()
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
                    is None -> Completable.error(NoSelectedCity)
                    is Some<City> -> weatherApiService.get5DayEvery3HourForecastByCityId(it.value.id)
                        .subscribeOn(Schedulers.io())
                        .flatMapCompletable(::saveFiveDayForecaseWeather)
                }
            }
    }
}