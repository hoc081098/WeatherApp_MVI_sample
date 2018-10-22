package com.hoc.weatherapp.ui.main.currentweather

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.*
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import java.util.concurrent.TimeUnit

private const val TAG = "^^&"

class CurrentWeatherPresenter(private val repository: Repository) :
  MviBasePresenter<View, ViewState>() {
  override fun bindIntents() {

    val refresh = intent(View::refreshCurrentWeatherIntent)
      .switchMap {
        repository.refreshCurrentWeather()
          .toObservable()
          .switchMap(::weather)
          .onErrorResumeNext(::showError)
      }
      .doOnNext { debug("refreshWeather $it", TAG) }

    val cityAndWeather = repository.getSelectedCityAndCurrentWeather()
      .toObservable()
      .publish { shared ->
        Observable.mergeArray(
          shared.ofType<None>().switchMap { showError(NoSelectedCityException) },
          shared.ofType<Some<CityAndCurrentWeather>>()
            .map { it.value }
            .map { it.currentWeather }
            .map { PartialChange.Weather(weather = it) }
            .cast<PartialChange>()
            .onErrorResumeNext(::showError)
        )
      }
      .doOnNext { debug("cityAndWeather $it", TAG) }

    subscribeViewState(
      Observable.mergeArray(refresh, cityAndWeather)
        .scan(ViewState(), ::reduce)
        .distinctUntilChanged()
        .doOnNext {
          debug(
            "CurrentWeatherPresenter ViewState = $it",
            TAG
          )
        }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private fun reduce(viewState: ViewState, partialChange: PartialChange): ViewState {
    return when (partialChange) {
      is PartialChange.Error -> viewState.copy(
        showError = partialChange.showMessage,
        error = partialChange.throwable,
        weather = if (partialChange.throwable is NoSelectedCityException) {
          null
        } else {
          viewState.weather
        }
      )
      is PartialChange.Weather -> viewState.copy(
        weather = partialChange.weather,
        error = null
      )
      is PartialChange.RefreshWeatherSuccess -> viewState.copy(
        weather = partialChange.weather,
        showRefreshSuccessfully = partialChange.showMessage,
        error = null
      )
    }
  }

  private fun showError(throwable: Throwable): Observable<PartialChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialChange.Error(throwable = throwable, showMessage = false)
      }
      .startWith(
        PartialChange.Error(throwable = throwable, showMessage = true)
      )
      .cast()
  }

  private fun weather(cityAndCurrentWeather: CityAndCurrentWeather): Observable<PartialChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialChange.RefreshWeatherSuccess(
          showMessage = false,
          weather = cityAndCurrentWeather.currentWeather
        )
      }
      .startWith(
        PartialChange.RefreshWeatherSuccess(
          showMessage = true,
          weather = cityAndCurrentWeather.currentWeather
        )
      )
      .cast()
  }
}