package com.hoc.weatherapp.ui.addcity

import android.app.Application
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CityRepository
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.ui.addcity.AddCityContract.View
import com.hoc.weatherapp.ui.addcity.AddCityContract.ViewState
import com.hoc.weatherapp.utils.MyUnsafeLazyImpl
import com.hoc.weatherapp.utils.checkLocationSettingAndGetCurrentLocation
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AddCityPresenter(
  private val cityRepository: CityRepository,
  private val currentWeatherRepository: CurrentWeatherRepository,
  private val application: Application
) :
  MviBasePresenter<View, ViewState>() {
  private val locationRequestLazy = MyUnsafeLazyImpl {
    LocationRequest()
      .setInterval(500)
      .setFastestInterval(500)
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
      .setNumUpdates(1)
  }
  private val locationSettingsRequestLazy = MyUnsafeLazyImpl {
    LocationSettingsRequest.Builder()
      .addLocationRequest(locationRequest)
      .build()
  }
  private val fusedLocationProviderClientLazy = MyUnsafeLazyImpl {
    LocationServices.getFusedLocationProviderClient(application)
  }
  private val settingsClientLazy = MyUnsafeLazyImpl {
    LocationServices.getSettingsClient(application)
  }

  private val locationRequest by locationRequestLazy
  private val fusedLocationProviderClient by fusedLocationProviderClientLazy
  private val locationSettingsRequest by locationSettingsRequestLazy
  private val settingsClient by settingsClientLazy

  private val tag = "_add_city_"

  private val addCityTransformer =
    ObservableTransformer<Pair<Double, Double>, ViewState> { intent ->
      intent
        .flatMap { (latitude, longitude) ->
          cityRepository
            .addCityByLatLng(latitude, longitude)
            .doOnSubscribe { debug("addCityByLatLng...", tag) }
            .doOnSuccess {
              currentWeatherRepository
                .refreshWeatherOf(it)
                .subscribeBy(onSuccess = {
                  debug("refreshWeatherOf success.. $it", tag)
                })
            }
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

  private val addCurrentLocationProcessor = ObservableTransformer<Unit, ViewState> { _ ->
    application.checkLocationSettingAndGetCurrentLocation(
      settingsClient = settingsClient,
      fusedLocationProviderClient = fusedLocationProviderClient,
      locationRequest = locationRequest,
      locationSettingsRequest = locationSettingsRequest
    ).subscribeOn(AndroidSchedulers.mainThread())
      .timeout(5_000, TimeUnit.MILLISECONDS) //  5 seconds timeout
      .retry { t1, t2 -> t2 is TimeoutException && t1 < 3 } // Try 3 times
      .toObservable()
      .doOnNext { debug("currentLocation $it", tag) }
      .map { it.latitude to it.longitude }
      .compose(addCityTransformer)
      .onErrorResumeNext { it: Throwable ->
        val throwable = if (it is TimeoutException) {
          TimeoutException("timeout to get current location. Try again!")
        } else {
          it
        }
        listOf(
          ViewState.Error(showMessage = true, throwable = throwable),
          ViewState.Error(showMessage = false, throwable = throwable)
        ).toObservable()
      }
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext { debug("viewState $it", tag) }
      .startWith(ViewState.Loading)
  }

  override fun bindIntents() {
    val addCurrentLocation = intent(View::addCurrentLocationIntent)
      .compose(addCurrentLocationProcessor)
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

  override fun unbindIntents() {
    super.unbindIntents()
    locationRequestLazy.cleanUp()
    locationSettingsRequestLazy.cleanUp()
    fusedLocationProviderClientLazy.cleanUp()
    settingsClientLazy.cleanUp()
  }
}