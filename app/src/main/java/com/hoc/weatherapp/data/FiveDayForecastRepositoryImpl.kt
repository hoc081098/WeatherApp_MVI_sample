package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.local.FiveDayForecastLocalDataSource
import com.hoc.weatherapp.data.local.SelectedCityPreference
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.data.remote.OpenWeatherMapApiService
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.Some
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class FiveDayForecastRepositoryImpl(
  private val openWeatherMapApiService: OpenWeatherMapApiService,
  private val fiveDayForecastLocalDataSource: FiveDayForecastLocalDataSource,
  private val selectedCityPreference: SelectedCityPreference
) : FiveDayForecastRepository {
 private val fiveDayForecastObservable: Observable<Optional<List<DailyWeather>>> = selectedCityPreference
    .observable
    .distinctUntilChanged()
    .switchMap { optional ->
      when (optional) {
        is Some -> fiveDayForecastLocalDataSource
          .getAllDailyWeathersByCityId(optional.value.id)
          .subscribeOn(Schedulers.io())
          .map(::Some)
        is None -> Observable.just(None)
      }
    }

  /**
   * Get stream of five day weather, get from local database
   * @return [Observable] emit [None] when having no selected city, otherwise emit [Some] of [DailyWeather]s
   */
  override fun getFiveDayForecastOfSelectedCity(): Observable<Optional<List<DailyWeather>>> {
    return fiveDayForecastObservable
  }

  /**
   * Refresh five day forecast of selected city, get from api
   * @return [Single] emit result or error, emit [NoSelectedCityException] when having no selected city
   */
  override fun refreshFiveDayForecastOfSelectedCity(): Single<List<DailyWeather>> {
    return Single.fromCallable { selectedCityPreference.value }
      .flatMap {
        when (it) {
          is None -> Single.error(NoSelectedCityException)
          is Some -> openWeatherMapApiService
            .get5DayEvery3HourForecastByCityId(it.value.id)
            .subscribeOn(Schedulers.io())
            .flatMap {
              LocalDataSourceUtil.saveFiveDayForecastWeather(
                fiveDayForecastLocalDataSource,
                it
              )
            }
        }
      }
  }
}