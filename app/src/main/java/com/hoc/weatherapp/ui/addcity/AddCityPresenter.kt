package com.hoc.weatherapp.ui.addcity

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.ui.addcity.AddCityContract.View
import com.hoc.weatherapp.ui.addcity.AddCityContract.ViewState
import com.hoc.weatherapp.utils.checkLocationSettingAndGetCurrentLocation
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.toObservable
import java.util.concurrent.TimeUnit

class AddCityPresenter(private val repository: Repository, private val application: Application) :
  MviBasePresenter<View, ViewState>() {
  private val tag = "_add_city_"
  private val addCityTransformer = ObservableTransformer<Pair<Double, Double>, ViewState> {
    it.flatMap { (latitude, longitude) ->
      repository.addCityByLatLng(latitude, longitude)
        .doOnSubscribe { debug("addCityByLatLng...", tag) }
        .toObservable()
        .flatMapIterable {
          listOf(
            ViewState.AddCitySuccessfully(city = it, showMessage = true),
            ViewState.AddCitySuccessfully(city = it, showMessage = false)
          )
        }
        .cast<ViewState>()
        .onErrorResumeNext { throwable: Throwable ->
          listOf(
            ViewState.Error(showMessage = true, throwable = throwable),
            ViewState.Error(showMessage = false, throwable = throwable)
          ).toObservable()
        }
        .observeOn(AndroidSchedulers.mainThread())
        .startWith(ViewState.Loading)
        .doOnNext { debug("addCityTransformer $it", tag) }
    }
  }

  override fun bindIntents() {
    val addCurrentLocation = intent(View::addCurrentLocationIntent)
      .flatMap {
        application.checkLocationSettingAndGetCurrentLocation()
          .subscribeOn(AndroidSchedulers.mainThread())
          .timeout(3_000, TimeUnit.MILLISECONDS)//TODO
          .toObservable()
          .doOnNext { debug("CurrentLocation $it", tag) }
          .map { it.latitude to it.longitude }
          .compose(addCityTransformer)
          .onErrorResumeNext { throwable: Throwable ->
            listOf(
              ViewState.Error(showMessage = true, throwable = throwable),
              ViewState.Error(showMessage = false, throwable = throwable)
            ).toObservable()
          }
          .observeOn(AndroidSchedulers.mainThread())
          .doOnNext { debug("ViewState $it", tag) }
          .startWith(ViewState.Loading)
      }
      .doOnNext { debug("addCurrentLocation $it", tag) }
      .doOnError { debug("addCurrentLocation $it", tag) }
      .doOnComplete { debug("addCurrentLocation", tag) }

    val addCityByLatLng = intent(View::addCityByLatLngIntent)
      .compose(addCityTransformer)

    subscribeViewState(
      Observable.mergeArray(addCityByLatLng, addCurrentLocation)
        .distinctUntilChanged()
        .doOnNext { debug("ViewState = $it", tag) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }
}