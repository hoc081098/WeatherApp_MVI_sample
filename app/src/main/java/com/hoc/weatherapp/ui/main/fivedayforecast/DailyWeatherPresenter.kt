package com.hoc.weatherapp.ui.main.fivedayforecast

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.View
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.ViewState
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.trim
import io.reactivex.android.schedulers.AndroidSchedulers

class DailyWeatherPresenter(private val repository: Repository) :
  MviBasePresenter<View, ViewState>() {
  private val tag = "_fivedayforecast_"

  override fun bindIntents() {
    val vs = repository.getFiveDayForecastOfSelectedCity()
      .map { it.getOrNull() ?: emptyList() }
      .map { weathers: List<DailyWeather> ->
        weathers
          .groupBy { it.timeOfDataForecasted.trim() }
          .toSortedMap()
          .flatMap { (date, weathers) ->
            listOf(DailyWeatherListItem.Header(date)) +
                weathers.map {
                  DailyWeatherListItem.Weather(
                    weatherIcon = it.icon,
                    weatherDescription = it.description.capitalize(),
                    temperatureMin = it.temperatureMin.toString(),
                    temperatureMax = it.temperatureMax.toString(),
                    dataTime = it.timeOfDataForecasted
                  )
                }
          }
      }
      .map { ViewState(it) }

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