package com.hoc.weatherapp.ui.main.fivedayforecast

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.View
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.ViewState
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.notOfType
import com.hoc.weatherapp.utils.trim
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.ofType

class DailyWeatherPresenter(
  private val repository: Repository,
  private val sharedPrefUtil: SharedPrefUtil
) :
  MviBasePresenter<View, ViewState>() {
  private val tag = "_five_day_forecast_"

  override fun bindIntents() {

    intent { it.refreshDailyWeatherIntent() }
      .publish { shared ->
        Observable.mergeArray(
          shared.ofType<DailyWeatherContract.RefreshIntent.InitialIntent>().take(1),
          shared.notOfType<DailyWeatherContract.RefreshIntent.InitialIntent>()
        )
      }
      .switchMap {
        repository.refreshFiveDayForecastOfSelectedCity()
          .toObservable()
          .onErrorReturnItem(emptyList())
      }
      .subscribe()

    val vs = Observables.combineLatest(
      repository.getFiveDayForecastOfSelectedCity()
        .map { it.getOrNull() ?: emptyList() },
      sharedPrefUtil.temperatureUnitObservable
    ).map { (weathers: List<DailyWeather>, temperatureUnit) ->
      weathers
        .groupBy { it.timeOfDataForecasted.trim() }
        .toSortedMap()
        .flatMap { (date, weathers) ->
          listOf(DailyWeatherListItem.Header(date)) +
              weathers.map {
                DailyWeatherListItem.Weather(
                  weatherIcon = it.icon,
                  weatherDescription = it.description.capitalize(),
                  temperatureMin = temperatureUnit.format(it.temperatureMin),
                  temperatureMax = temperatureUnit.format(it.temperatureMax),
                  dataTime = it.timeOfDataForecasted
                )
              }
        }
    }.map { ViewState(it) }

    subscribeViewState(
      vs.distinctUntilChanged()
        .doOnNext {
          debug("ViewState=$it", tag)
        }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }
}