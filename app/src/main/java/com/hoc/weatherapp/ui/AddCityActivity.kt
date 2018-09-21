package com.hoc.weatherapp.ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.SharedPrefUtil
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.snackBar
import com.hoc.weatherapp.utils.toFlowable
import com.hoc.weatherapp.utils.toast
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_add_city.*
import kotlinx.android.synthetic.main.some_city_layout.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import androidx.core.app.ActivityCompat.requestPermissions as requestPermissionsCompat

class AddCityActivity : AppCompatActivity() {
    private val fusedLocationProviderClient by lazy(LazyThreadSafetyMode.NONE) {
        LocationServices.getFusedLocationProviderClient(this@AddCityActivity)
    }
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private val weatherRepository by inject<WeatherRepository>()

    private val compositeDisposable = CompositeDisposable()
    private var countRequestLocationPermission = 0
    private val localBroadcastManager by lazy(LazyThreadSafetyMode.NONE) {
        LocalBroadcastManager.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_city)

        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = "Add a city"
        }

        countRequestLocationPermission =
            savedInstanceState?.getInt(COUNT_REQUEST_LOCATION_PERMISSION) ?: 0

        setupButtonMyLocation()
        setupAutoCompletePlace()
    }

    private fun setupAutoCompletePlace() {
        (supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as SupportPlaceAutocompleteFragment).run {
            setHint("Search city ...")
            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place?) {
                    val latitude = place?.latLng?.latitude
                    val longitude = place?.latLng?.longitude

                    if (latitude != null && longitude != null) {
                        weatherRepository.getCityInformationAndSaveToLocal(latitude, longitude)
                            .subscribe_()
                    }
                }

                override fun onError(status: Status?) {}
            })
        }
    }

    private fun setupButtonMyLocation() {
        val processor = PublishProcessor.create<Unit>()
        button_my_loction.setOnClickListener { processor.onNext(Unit) }
        processor.hide()
            .throttleFirst(600, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {},
                onNext = { addCurrentLocation() }
            )
            .addTo(compositeDisposable)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(
            COUNT_REQUEST_LOCATION_PERMISSION,
            countRequestLocationPermission
        )
    }

    private fun Flowable<City>.subscribe_() {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                debug("doOnSubscribe")
                TransitionManager.beginDelayedTransition(
                    root_add_city_activity,
                    TransitionSet()
                        .addTransition(Fade(Fade.OUT).addTarget(button_my_loction))
                        .addTransition(Fade(Fade.IN).addTarget(progress_bar))
                        .setDuration(200)
                )
                button_my_loction.visibility = View.INVISIBLE
                progress_bar.visibility = View.VISIBLE
            }
            .subscribeBy(
                onError = {
                    root_add_city_activity.snackBar("Add city error: ${it.message}")
                    hideProgressBar()
                },
                onNext = {
                    toast("Add city successfully!")
                    hideProgressBar()

                    sharedPrefUtil.selectedCity = it
                    localBroadcastManager
                        .sendBroadcast(
                            Intent(ACTION_CHANGED_LOCATION).apply {
                                putExtra(SELECTED_CITY, it)
                            }
                        )
                }
            )
            .addTo(compositeDisposable)
    }

    private fun hideProgressBar() {
        TransitionManager.beginDelayedTransition(
            root_add_city_activity,
            TransitionSet()
                .addTransition(Fade(Fade.IN).addTarget(button_my_loction))
                .addTransition(Fade(Fade.OUT).addTarget(progress_bar))
                .setDuration(200)
        )
        progress_bar.visibility = View.INVISIBLE
        button_my_loction.visibility = View.VISIBLE
    }

    private fun addCurrentLocation() {
        when {
            checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                && checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED -> {
                if (countRequestLocationPermission < MAX_NUMBER_REQUEST_PERMISSON) {
                    ++countRequestLocationPermission

                    requestPermissionsCompat(
                        this, arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                        LOCATION_PERMISSION_RC
                    )
                } else {
                    root_add_city_activity.snackBar("Go to setting to grant location permission")
                }
            }
            else -> {
                fusedLocationProviderClient.lastLocation
                    .toFlowable()
                    .flatMap {
                        weatherRepository.getCityInformationAndSaveToLocal(
                            it.latitude,
                            it.longitude
                        ).subscribeOn(Schedulers.io())
                    }
                    .subscribe_()
            }
        }
    }

    /*
    private fun enableGps() {
        val googleApiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                }

                override fun onConnectionSuspended(p0: Int) {
                }
            })
            .addOnConnectionFailedListener {

            }
            .build()
            .apply {
                connect()
            }
        val locationRequest = LocationRequest()
            .apply {
                priority = PRIORITY_HIGH_ACCURACY
                interval = 30 * 1_000
                fastestInterval = 5 * 1_000
            }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
        @Suppress("DEPRECATION")
        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
            .setResultCallback {
                val status = it.status
                when (status.statusCode) {
                    LocationSettingsStatusCodes.SUCCESS -> {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                ACCESS_FINE_LOCATION
                            ) != PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(
                                this,
                                ACCESS_COARSE_LOCATION
                            ) != PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                                LOCATION_PERMISSION_RC1
                            )
                        } else {
                            getNearCity()
                        }
                    }
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                        } catch (e: IntentSender.SendIntentException) {

                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            REQUEST_CHECK_SETTINGS -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                ACCESS_FINE_LOCATION
                            ) != PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(
                                this,
                                ACCESS_COARSE_LOCATION
                            ) != PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                                LOCATION_PERMISSION_RC1
                            )
                        } else {
                            getNearCity()
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        enableGps()
                    }
                }
            }
        }
    }

    private fun getNearCity() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
        ) return

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener {
                when (it) {
                    null -> toast("Can't get location. Please enable GPS")
                    else -> {
                        Geocoder(this, Locale.getDefault())
                            .getFromLocation(
                                it.latitude,
                                it.longitude,
                                1
                            )
                            .firstOrNull()
                            ?.countryCode
                            ?.let(::getCityByCountryCode)
                    }
                }
            }
            .addOnFailureListener {

            }
    }

    private fun getCityByCountryCode(country: String) {}
    */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_RC -> addCurrentLocation()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> true.also { finish() }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    companion object {
        private const val LOCATION_PERMISSION_RC = 2
        private const val COUNT_REQUEST_LOCATION_PERMISSION =
            "COUNT_REQUEST_LOCATION_PERMISSION"

        /*
        private const val LOCATION_PERMISSION_RC1 = 1
        private const val REQUEST_CHECK_SETTINGS = 3
        */

        private const val MAX_NUMBER_REQUEST_PERMISSON = 2

        const val ACTION_CHANGED_LOCATION = "ACTION_CHANGED_LOCATION"
        const val SELECTED_CITY = "SELECTED_CITY"
    }
}
