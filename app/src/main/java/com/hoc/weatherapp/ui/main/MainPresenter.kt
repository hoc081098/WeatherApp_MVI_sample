package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.CityAndWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.NoSelectedCity
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType

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

  override fun unbindIntents() {
    super.unbindIntents()
    compositeDisposable.clear()
  }
}