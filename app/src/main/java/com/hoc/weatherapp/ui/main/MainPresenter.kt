package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.ui.main.MainContract.ViewState.CityAndWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.NoSelectedCity
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

class MainPresenter(
  private val currentWeatherRepository: CurrentWeatherRepository,
  private val colorHolderSource: ColorHolderSource
) : MviBasePresenter<MainContract.View, MainContract.ViewState>() {
  private val tag = "main"
  private val compositeDisposable = CompositeDisposable()

  override fun bindIntents() {
    intent(MainContract.View::changeVibrantColorIntent)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(colorHolderSource::change)
      .addTo(compositeDisposable)

    val cityAndCurrentWeather = currentWeatherRepository
      .getSelectedCityAndCurrentWeatherOfSelectedCity()
      .map {
        when (it) {
          None -> NoSelectedCity
          is Some -> CityAndWeather(
            city = it.value.city,
            weather = it.value.currentWeather
          )
        }
      }
      .distinctUntilChanged()
      .doOnNext { debug("MainPresenter state=$it", tag) }
      .observeOn(AndroidSchedulers.mainThread())

    subscribeViewState(cityAndCurrentWeather, MainContract.View::render)
  }

  override fun unbindIntents() {
    super.unbindIntents()
    compositeDisposable.clear()
  }
}