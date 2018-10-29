package com.hoc.weatherapp.ui.main.currentweather

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.*
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.work.WorkerUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import java.util.concurrent.TimeUnit

private const val TAG = "currrentweather"

class CurrentWeatherPresenter(
  private val repository: Repository,
  private val androidApplication: Application,
  private val sharedPrefUtil: SharedPrefUtil
) :
  MviBasePresenter<View, ViewState>() {
  override fun bindIntents() {
    subscribeViewState(
      Observable.mergeArray(refreshWeatherPartialChange(), cityAndWeatherPartialChange())
        .scan(ViewState(), ::reduce)
        .distinctUntilChanged()
        .doOnNext { debug("CurrentWeatherPresenter ViewState = $it", TAG) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private fun cityAndWeatherPartialChange(): Observable<PartialStateChange> {
    val cityAndWeather = repository.getSelectedCityAndCurrentWeatherOfSelectedCity()
      .publish { shared ->
        Observable.mergeArray(
          shared.ofType<None>().switchMap { showError(NoSelectedCityException) },
          shared.ofType<Some<CityAndCurrentWeather>>()
            .map { it.value }
            .map { it.currentWeather }
            .map { PartialStateChange.Weather(weather = it) }
            .cast<PartialStateChange>()
            .onErrorResumeNext(::showError)
        )
      }
      .doOnNext { debug("cityAndWeather $it", TAG) }
    return cityAndWeather
  }

  private fun refreshWeatherPartialChange(): Observable<PartialStateChange> {
    return intent(View::refreshCurrentWeatherIntent)
      .publish { shared ->
        Observable.mergeArray(
          shared.ofType<RefreshIntent.InitialRefreshIntent>().take(1),
          shared.notOfType<RefreshIntent.InitialRefreshIntent>()
        )
      }
      .doOnNext { debug("refresh intent $it") }
      .switchMap {
        repository.refreshCurrentWeatherOfSelectedCity()
          .doOnSuccess {
            WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
            if (sharedPrefUtil.showNotification) {
              androidApplication.showOrUpdateNotification(
                cityName = it.city.name,
                unit = sharedPrefUtil.temperatureUnit,
                cityCountry = it.city.country,
                weather = it.currentWeather
              )
            }
          }
          .doOnError {
            if (it is NoSelectedCityException) {
              androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
              WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
            }
          }
          .toObservable()
          .observeOn(AndroidSchedulers.mainThread())
          .switchMap(::showWeather)
          .onErrorResumeNext(::showError)
      }
      .doOnNext { debug("refreshWeather $it", TAG) }
  }

  private fun reduce(viewState: ViewState, partialStateChange: PartialStateChange): ViewState {
    return when (partialStateChange) {
      is PartialStateChange.Error -> viewState.copy(
        showError = partialStateChange.showMessage,
        error = partialStateChange.throwable,
        weather = if (partialStateChange.throwable is NoSelectedCityException) {
          null
        } else {
          viewState.weather
        }
      )
      is PartialStateChange.Weather -> viewState.copy(
        weather = partialStateChange.weather,
        error = null
      )
      is PartialStateChange.RefreshWeatherSuccess -> viewState.copy(
        weather = partialStateChange.weather,
        showRefreshSuccessfully = partialStateChange.showMessage,
        error = null
      )
    }
  }

  private fun showError(throwable: Throwable): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialStateChange.Error(throwable = throwable, showMessage = false)
      }
      .startWith(
        PartialStateChange.Error(throwable = throwable, showMessage = true)
      )
      .cast()
  }

  private fun showWeather(cityAndCurrentWeather: CityAndCurrentWeather): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialStateChange.RefreshWeatherSuccess(
          showMessage = false,
          weather = cityAndCurrentWeather.currentWeather
        )
      }
      .startWith(
        PartialStateChange.RefreshWeatherSuccess(
          showMessage = true,
          weather = cityAndCurrentWeather.currentWeather
        )
      )
      .cast()
  }
}