package com.hoc.weatherapp.ui.addcity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.ui.BaseMviActivity
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.toast
import com.jakewharton.rxbinding3.view.clicks
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_add_city.*
import kotlinx.android.synthetic.main.some_city_layout.*
import org.koin.android.ext.android.get
import java.util.concurrent.TimeUnit

class AddCityActivity : BaseMviActivity<AddCityContract.View, AddCityPresenter>(),
    AddCityContract.View {
  private val tag = "addcity"
  private val publishSubjectAutoCompletePlace = PublishSubject.create<Pair<Double, Double>>()
  private val publishSubjectTriggerAddCurrentLocation = PublishSubject.create<Unit>()

  override fun addCurrentLocationIntent(): Observable<Unit> {
    return button_my_loction.clicks()
        .throttleFirst(600, TimeUnit.MILLISECONDS)
        .compose(
            RxPermissions(this).ensureEach(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
        .filter { it.granted }
        .map { Unit }
        .mergeWith(publishSubjectTriggerAddCurrentLocation)
        .doOnNext { debug("button my location clicks", tag) }
  }

  override fun addCityByLatLngIntent(): Observable<Pair<Double, Double>> {
    return publishSubjectAutoCompletePlace
        .doOnNext { debug("publishSubjectAutoCompletePlace $it", tag) }
  }

  override fun createPresenter() = get<AddCityPresenter>()

  override fun render(state: AddCityContract.ViewState) {
    when (state) {
      AddCityContract.ViewState.Loading -> showProgressbar()
      is AddCityContract.ViewState.AddCitySuccessfully -> {
        hideProgressbar()
        if (state.showMessage) toast("Added ${state.city.name}")
      }
      is AddCityContract.ViewState.Error -> {
        hideProgressbar()
        if (state.showMessage) {
          toast("Error ${state.throwable.message}")
          if (state.throwable is ResolvableApiException) {
            runCatching {
              state.throwable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
            }
          }
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add_city)

    setSupportActionBar(toolbar)
    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      title = "Add a city"
    }

    setupAutoCompletePlace()
  }

  private fun setupAutoCompletePlace() {
    (supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as SupportPlaceAutocompleteFragment).run {
      setHint("Search city ...")
      setOnPlaceSelectedListener(object : PlaceSelectionListener {
        override fun onPlaceSelected(place: Place?) {
          val latitude = place?.latLng?.latitude ?: return
          val longitude = place.latLng?.longitude ?: return
          publishSubjectAutoCompletePlace.onNext(latitude to longitude)
        }

        override fun onError(status: Status) {
          toast(status.statusMessage ?: "An error occurred")
        }
      })
    }
  }

  private fun showProgressbar() {
    TransitionManager.beginDelayedTransition(
        findViewById(android.R.id.content),
        TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(Fade(Fade.IN).addTarget(progress_bar))
            .setInterpolator(AccelerateDecelerateInterpolator())
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
            .addTransition(ChangeBounds())
            .setInterpolator(AccelerateDecelerateInterpolator())
    )

    progress_bar.visibility = View.INVISIBLE
    button_my_loction.visibility = View.VISIBLE
    (button_my_loction.layoutParams as ConstraintLayout.LayoutParams).run {
      width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> true.also { finish() }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    debug("requestCode = [$requestCode], resultCode = [$resultCode], data = [$data]", "789654")
    when (requestCode) {
      REQUEST_CHECK_SETTINGS -> if (resultCode == Activity.RESULT_OK) {
        publishSubjectTriggerAddCurrentLocation.onNext(Unit)
      }
    }
  }

  companion object {
    const val REQUEST_CHECK_SETTINGS = 1
  }
}
