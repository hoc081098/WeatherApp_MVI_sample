package com.hoc.weatherapp.ui.addcity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import android.view.View
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
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.toast
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
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

  private fun getCurrentLocation(): Single<Location> {
    val publishProcessor = PublishSubject.create<Location>()

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
          publishProcessor.onError(IllegalStateException("Need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        fusedLocationProviderClient.lastLocation
          .addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
              publishProcessor.onNext(lastLocation)
              publishProcessor.onComplete()
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
          publishProcessor.onError(IllegalStateException("Need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        fusedLocationProviderClient.requestLocationUpdates(
          locationRequest,
          object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
              val lastLocation = locationResult?.lastLocation ?: return
              fusedLocationProviderClient.removeLocationUpdates(this)
              publishProcessor.onNext(lastLocation)
              publishProcessor.onComplete()
            }
          },
          null
        )
      }.addOnFailureListener {
        if (it is ResolvableApiException) {
          runCatching { it.startResolutionForResult(this, REQUEST_CHECK_SETTINGS) }
        }
        publishProcessor.onError(IllegalStateException("Location settings are not satisfied"))
      }
    return publishProcessor.take(1).singleOrError()
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

        override fun onError(status: Status?) {
          toast(status?.statusMessage ?: "An error occurred")
        }
      })
    }
    publishSubject.addCity()
  }

  private fun setupButtonMyLocation() {
    val onClick = PublishSubject.create<Unit>()
    button_my_loction.setOnClickListener { onClick.onNext(Unit) }

    Observable
      .mergeArray(
        onClick
          .throttleFirst(600, TimeUnit.MILLISECONDS),
        triggerAddCurrentLocation
      ).compose(
        RxPermissions(this).ensureEach(
          Manifest.permission.ACCESS_FINE_LOCATION
        )
      )
      .filter { it.granted }
      .doOnNext { debug("permission $it", "789654") }
      .flatMapSingle { getCurrentLocation() }
      .map { it.latitude to it.longitude }
      .addCity()
  }

  private fun Observable<Pair<Double, Double>>.addCity() {
    flatMap { (latitude, longitude) ->
      repository.addCityByLatLng(
        latitude,
        longitude
      ).toObservable()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { showProgressbar() }
        .doOnTerminate { hideProgressbar() }
    }.map { Result.success(it) }
      .onErrorReturn { Result.failure(it) }
      .subscribeBy(
        onNext = {
          it.onSuccess { toast("Added $it") }
            .onFailure { toast(it.message ?: "An error occurred") }
        }
      ).addTo(compositeDisposable)
  }

  private fun showProgressbar() {
    val set1 = TransitionSet()
      .addTransition(ChangeBounds())
      .addTransition(Fade(Fade.OUT))
      .addTarget(button_my_loction!!)
      .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)

    TransitionManager.beginDelayedTransition(
      findViewById(android.R.id.content),
      TransitionSet()
        .addTransition(set1)
        .addTransition(Fade(Fade.IN).addTarget(progress_bar))
        .setDuration(300)
        .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
    )

    (button_my_loction.layoutParams as ConstraintLayout.LayoutParams).width =
        ConstraintLayout.LayoutParams.WRAP_CONTENT
    button_my_loction.visibility = View.INVISIBLE
    progress_bar.visibility = View.VISIBLE
  }

  private fun hideProgressbar() {
    val set1 = TransitionSet()
      .addTransition(Fade(Fade.IN))
      .addTransition(ChangeBounds())
      .addTarget(button_my_loction)
      .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)

    TransitionManager.beginDelayedTransition(
      findViewById(android.R.id.content),
      TransitionSet()
        .addTransition(Fade(Fade.OUT).addTarget(progress_bar))
        .addTransition(set1)
        .setDuration(300)
        .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
    )

    progress_bar.visibility = View.INVISIBLE
    button_my_loction.visibility = View.VISIBLE
    (button_my_loction.layoutParams as ConstraintLayout.LayoutParams).width =
        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      android.R.id.home -> true.also { finish() }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_CHECK_SETTINGS -> if (resultCode == Activity.RESULT_OK) {
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
