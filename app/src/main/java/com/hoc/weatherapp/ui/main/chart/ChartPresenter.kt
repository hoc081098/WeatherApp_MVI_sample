package com.hoc.weatherapp.ui.main.chart

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.ui.main.chart.ChartContract.View
import com.hoc.weatherapp.ui.main.chart.ChartContract.ViewState
import com.hoc.weatherapp.ui.main.fivedayforecast.Tuple4
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables

class ChartPresenter(
  private val repository: Repository,
  private val sharedPrefUtil: SharedPrefUtil
) : MviBasePresenter<View, ViewState>() {
  override fun bindIntents() {
    val viewState = Observables.combineLatest(
      source1 = repository.getFiveDayForecastOfSelectedCity(),
      source2 = sharedPrefUtil.temperatureUnitObservable,
      source3 = sharedPrefUtil.speedUnitObservable,
      source4 = sharedPrefUtil.pressureUnitObservable,
      combineFunction = { optional, temperatureUnit, speedUnit, pressureUnit ->
        Tuple4(
          temperatureUnit = temperatureUnit,
          weathers = optional.getOrNull().orEmpty(),
          pressureUnit = pressureUnit,
          speedUnit = speedUnit
        )
      }
    ).map {
      ViewState(
        temperatureUnit = it.temperatureUnit,
        weathers = it.weathers,
        speedUnit = it.speedUnit,
        pressureUnit = it.pressureUnit
      )
    }.distinctUntilChanged()
      .doOnNext { debug("ChartPresenter ViewState=$it", "$#$#$#$#$#") }
      .observeOn(AndroidSchedulers.mainThread())

    subscribeViewState(viewState, View::render)
  }
}