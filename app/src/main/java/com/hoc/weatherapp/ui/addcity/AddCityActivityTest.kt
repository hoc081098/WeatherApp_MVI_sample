package com.hoc.weatherapp.ui.addcity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.toast
import com.jakewharton.rxbinding3.view.clicks
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_add_city.*
import kotlinx.android.synthetic.main.some_city_layout.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class AddCityActivityTest : AppCompatActivity() {
  private val compositeDisposable = CompositeDisposable()
  private val repository by inject<Repository>()
  private val triggerAddCurrentLocation = PublishSubject.create<Unit>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add_city)

    setSupportActionBar(toolbar)
    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      title = "Add a city"
    }

    setupButtonMyLocation()
    setupAutoCompletePlace()
  }

  private fun getCurrentLocation(): Observable<Location> {
    val publishSubject = PublishSubject.create<Location>()

    val locationRequest = LocationRequest()
      .setInterval(1000)
      .setFastestInterval(500)
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
      .setNumUpdates(1)
    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    LocationServices.getSettingsClient(this)
      .checkLocationSettings(
        LocationSettingsRequest.Builder()
          .addLocationRequest(locationRequest)
          .build()
      )
      .addOnSuccessListener {
        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
          ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
          ) != PackageManager.PERMISSION_GRANTED
        ) {
          publishSubject.onError(IllegalStateException("Need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        fusedLocationProviderClient.lastLocation
          .addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
              publishSubject.onNext(lastLocation)
              publishSubject.onComplete()
            }
          }

        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
          ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
          ) != PackageManager.PERMISSION_GRANTED
        ) {
          publishSubject.onError(IllegalStateException("Need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        fusedLocationProviderClient.requestLocationUpdates(
          locationRequest,
          object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
              val lastLocation = locationResult?.lastLocation ?: return
              fusedLocationProviderClient.removeLocationUpdates(this)
              publishSubject.onNext(lastLocation)
              publishSubject.onComplete()
            }
          },
          null /* LOOPER */
        )
      }.addOnFailureListener {
        if (it is ResolvableApiException) {
          runCatching { it.startResolutionForResult(this, REQUEST_CHECK_SETTINGS) }
        }
        publishSubject.onError(IllegalStateException("Location settings are not satisfied"))
      }
    return publishSubject.take(1)
  }

  private fun setupAutoCompletePlace() {
    val publishSubject = PublishSubject.create<Pair<Double, Double>>()
    (supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as SupportPlaceAutocompleteFragment).run {
      setHint("Search city ...")
      setOnPlaceSelectedListener(object : PlaceSelectionListener {
        override fun onPlaceSelected(place: Place?) {
          val latitude = place?.latLng?.latitude ?: return
          val longitude = place.latLng?.longitude ?: return
          publishSubject.onNext(latitude to longitude)
        }

        override fun onError(status: Status) {
          toast(status.statusMessage ?: "An error occurred")
        }
      })
    }
    publishSubject
      .compose(addCity)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onError = {toast(it.toString())},
        onNext = {
          it.onSuccess {
            toast("Added ${it.name}")
          }.onFailure {
            toast("Error ${it.message}")
          }
        },
        onComplete = {toast("onComplete")}
      )
  }

  private fun setupButtonMyLocation() {
    Observable
      .mergeArray(
        button_my_loction.clicks()
          .throttleFirst(600, TimeUnit.MILLISECONDS)
          .doOnNext { debug("button my location clicks", "789654") },
          triggerAddCurrentLocation
      )
      .doOnNext { debug("oh yeah", "789654") }
      .compose(
        RxPermissions(this).ensureEach(
          Manifest.permission.ACCESS_FINE_LOCATION
        )
      )
      .doOnNext { debug("after compose $it", "789654") }
      .filter { it.granted }
      .doOnNext { debug("after filter { it.granted } $it", "789654") }
      .flatMap {
        getCurrentLocation()
          .map { location -> location.latitude to location.longitude }
          .compose(addCity)
          .onErrorResumeNext { throwable: Throwable -> Observable.just(Result.failure(throwable)) }
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSubscribe { showProgressbar() }
      }
      .subscribeBy(
        onError = {toast(it.toString())},
        onNext = {
          it.onSuccess {
            toast("Added ${it.name}")
          }.onFailure {
            toast("Error ${it.message}")
          }
          hideProgressbar()
        },
        onComplete = {toast("onComplete")}
      )
  }

  private val addCity = ObservableTransformer<Pair<Double, Double>, Result<City>>{
      it.flatMap { (latitude, longitude) ->
        repository.addCityByLatLng(latitude, longitude)
          .toObservable()
          .map { Result.success(it) }
          .onErrorResumeNext { it: Throwable -> Observable.just(Result.failure(it)) }
          .doOnSubscribe { debug("doOnSubscribe addCityByLatLng", "789654") }
          .doOnNext { debug("doOnNext $it addCityByLatLng", "789654") }
      }
  }

  private fun showProgressbar() {
    TransitionManager.beginDelayedTransition(
      findViewById(android.R.id.content),
      TransitionSet()
        .addTransition(ChangeBounds().setInterpolator(AccelerateDecelerateInterpolator()))
        .addTransition(Fade(Fade.IN).addTarget(progress_bar))
    )

    (button_my_loction.layoutParams as ConstraintLayout.LayoutParams).run {
      width = height
    }
    button_my_loction.visibility = View.INVISIBLE
    progress_bar.visibility = View.VISIBLE
  }

  private fun hideProgressbar() {
    TransitionManager.beginDelayedTransition(
      findViewById(android.R.id.content),
      TransitionSet()
        .addTransition(Fade(Fade.OUT).addTarget(progress_bar))
        .addTransition(ChangeBounds().setInterpolator(AccelerateDecelerateInterpolator()))
    )

    progress_bar.visibility = View.INVISIBLE
    button_my_loction.visibility = View.VISIBLE
    (button_my_loction.layoutParams as ConstraintLayout.LayoutParams).run {
      width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
    }
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      android.R.id.home -> true.also { finish() }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    debug("requestCode = [$requestCode], resultCode = [$resultCode], data = [$data]", "789654")
    when (requestCode) {
      REQUEST_CHECK_SETTINGS -> if (resultCode == Activity.RESULT_OK) {
        debug("trigger", "789654")
        triggerAddCurrentLocation.onNext(Unit)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }

  companion object {
    const val REQUEST_CHECK_SETTINGS = 1
  }
}
