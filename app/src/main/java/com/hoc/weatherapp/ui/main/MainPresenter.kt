package com.hoc.weatherapp.ui.main

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.CityAndWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.NoSelectedCity
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.ofType

class MainPresenter(
  private val repository: Repository,
  private val sharedPrefUtil: SharedPrefUtil,
  private val application: Application
) :
  MviBasePresenter<MainContract.View, MainContract.ViewState>() {
  private val tag = "main"

  override fun bindIntents() {


    val cityAndCurrentWeather = repository
      .getSelectedCityAndCurrentWeatherOfSelectedCity()
      .publish { shared ->
        Observable.merge(
          shared.ofType<Some<CityAndCurrentWeather>>()
            .map { it.value }
            .map {
              CityAndWeather(
                city = it.city,
                weather = it.currentWeather
              )
            },
          shared.ofType<None>().map { NoSelectedCity }
        )
      }
      .distinctUntilChanged()
      .doOnNext { debug("MainPresenter state=$it", tag) }
      .observeOn(AndroidSchedulers.mainThread())

    subscribeViewState(cityAndCurrentWeather, MainContract.View::render)
  }
}