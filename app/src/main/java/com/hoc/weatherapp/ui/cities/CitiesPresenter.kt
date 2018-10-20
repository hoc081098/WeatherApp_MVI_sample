package com.hoc.weatherapp.ui.cities

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.ui.cities.CitiesContract.View
import com.hoc.weatherapp.ui.cities.CitiesContract.ViewState
import com.hoc.weatherapp.ui.cities.CitiesContract.ViewState.CityListItems
import com.hoc.weatherapp.ui.cities.CitiesContract.ViewState.Error
import com.hoc.weatherapp.utils.getOrNull
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables.combineLatest
import io.reactivex.rxkotlin.cast
import java.util.concurrent.TimeUnit

class CitiesPresenter(private val repository: Repository) : MviBasePresenter<View, ViewState>() {
  override fun bindIntents() {
    val selectedCity = repository.getSelectedCity().toObservable()
    val cityAndCurrentWeathers = intent(View::searchStringIntent)
      .switchMap {
        repository.getAllCityAndCurrentWeathers(it)
          .toObservable()
      }

    val cityListItems = combineLatest(
      selectedCity,
      cityAndCurrentWeathers
    ).map { (city, list) ->
      list.map {
        CityListItem(
          city = it.city,
          weatherIcon = it.currentWeather.icon,
          weatherConditionId = it.currentWeather.weatherConditionId,
          isSelected = it.city.id == city.getOrNull()?.id,
          temperatureMax = it.currentWeather.temperatureMax.toString(),
          temperatureMin = it.currentWeather.temperatureMin.toString(),
          weatherDescription = it.currentWeather.description
        )
      }
    }.map(::CityListItems)
      .cast<ViewState>()
      .onErrorResumeNext(::showError)

    subscribeViewState(
      Observable.mergeArray(
        cityListItems
      ).distinctUntilChanged().observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private fun showError(throwable: Throwable): ObservableSource<ViewState> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map { Error(showMessage = false, throwable = throwable) }
      .startWith(Error(showMessage = true, throwable = throwable))
      .cast()
  }
}

