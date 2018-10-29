package com.hoc.weatherapp.ui.main.chart

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.ui.main.chart.ChartContract.View
import com.hoc.weatherapp.ui.main.chart.ChartContract.ViewState
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
      repository.getFiveDayForecastOfSelectedCity(),
      sharedPrefUtil.temperatureUnitObservable
    ).map {
      ViewState(
        temperatureUnit = it.second,
        weathers = it.first.getOrNull()
      )
    }.distinctUntilChanged()
      .doOnNext { debug("ChartPresenter ViewState=$it", "$#$#$#$#$#") }
      .observeOn(AndroidSchedulers.mainThread())

    subscribeViewState(viewState, View::render)
  }
}