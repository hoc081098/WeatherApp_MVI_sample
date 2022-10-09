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
import com.hoc.weatherapp.utils.exhaustMap
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
) : MviBasePresenter<View, ViewState>() {
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

  private val addCityTransformer =
    ObservableTransformer<Pair<Double, Double>, ViewState> { latLng ->
      latLng
        .flatMap { (latitude, longitude) ->
          cityRepository
            .addCityByLatLng(latitude, longitude)
            .doOnSuccess { city ->
              currentWeatherRepository
                .refreshWeatherOf(city)
                .subscribeBy(
                  onSuccess = { debug("refreshWeatherOf success.. $it", TAG) },
                  onError = { debug("refreshWeatherOf failure... $it", TAG, it) }
                )
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
        }
    }

  private val addCurrentLocationProcessor =
    ObservableTransformer<Unit, ViewState> { addCurrentLocationIntent ->
      addCurrentLocationIntent
        .exhaustMap {
          application
            .checkLocationSettingAndGetCurrentLocation(
              settingsClient = settingsClient,
              fusedLocationProviderClient = fusedLocationProviderClient,
              locationRequest = locationRequest,
              locationSettingsRequest = locationSettingsRequest
            )
            .subscribeOn(AndroidSchedulers.mainThread())
            .timeout(5_000, TimeUnit.MILLISECONDS) //  5 seconds timeout
            .retry { count, throwable -> throwable is TimeoutException && count < 3 } // Try 3 times
            .toObservable()
            .map { it.latitude to it.longitude }
            .compose(addCityTransformer)
            .onErrorResumeNext { throwable: Throwable ->
              val newThrowable = if (throwable is TimeoutException) {
                TimeoutException("timeout to get current location. Try again!")
              } else {
                throwable
              }
              listOf(
                ViewState.Error(showMessage = true, throwable = newThrowable),
                ViewState.Error(showMessage = false, throwable = newThrowable)
              ).toObservable()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .startWith(ViewState.Loading)
        }
    }

  override fun bindIntents() {
    val addCurrentLocation =
      intent(View::addCurrentLocationIntent).compose(addCurrentLocationProcessor)
    val addCityByLatLng = intent(View::addCityByLatLngIntent).compose(addCityTransformer)

    subscribeViewState(
      Observable.mergeArray(addCityByLatLng, addCurrentLocation)
        .distinctUntilChanged()
        .doOnNext { debug("viewState = $it", TAG) }
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

  private companion object {
    private const val TAG = "__add_city__"
  }
}
