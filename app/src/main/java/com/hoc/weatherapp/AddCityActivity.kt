package com.hoc.weatherapp

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment
import com.hoc.weatherapp.data.City
import com.hoc.weatherapp.data.Weather
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_add_city.*
import kotlinx.android.synthetic.main.some_city_layout.*
import org.koin.android.ext.android.inject
import java.util.*

class AddCityActivity : AppCompatActivity() {
    private val fusedLocationProviderClient by lazy(LazyThreadSafetyMode.NONE) {
        LocationServices.getFusedLocationProviderClient(this@AddCityActivity)
    }
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private val weatherRepository by inject<WeatherRepository>()

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_city)

        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = "Add a city"
        }

        enableGps()

        button_my_loc.setOnClickListener {
            getCurrentLocation()
        }

        val autocompleteFragment =
                supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment)
                        as SupportPlaceAutocompleteFragment
        autocompleteFragment.run {
            setHint("Search city")
            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place?) {
                    val id = place?.id
                    val latitude = place?.latLng?.latitude
                    val longitude = place?.latLng?.longitude

                    if (id !== null && latitude != null && longitude != null) {
                        addLocationFromAutoCompleteFragment(id, latitude, longitude)
                    }
                }

                override fun onError(status: Status?) {}
            })
        }
    }

    private fun addLocationFromAutoCompleteFragment(id: String, lat: Double, lng: Double) {
        debug("Id1 = $id")
        getCityInformation(lat, lng)
    }

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

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_RC2
            )
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
        ) return

        fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    when {
                        location === null -> toast("Can't get location. Please enable GPS")
                        else -> getCityInformation(location.latitude, location.longitude)
                    }

                }
                .addOnFailureListener {
                    toast(it.message ?: "Unknown error occurred!")
                }
    }

    private fun getCityInformation(latitude: Double, longitude: Double) {
        City(lat = latitude, lng = longitude)
                .let(weatherRepository::getCurrentWeatherByCity)
                .map(Weather::city)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onError = {},
                        onNext = {
                            debug("Id2 = ${it.id}")
                            sharedPrefUtil.selectedCity = it
                            LocalBroadcastManager.getInstance(this@AddCityActivity)
                                    .sendBroadcast(
                                            Intent(ACTION_CHANGED_LOCATION).apply {
                                                putExtra(SELECTED_CITY, it)
                                            }
                                    )
                        }
                )
                .addTo(compositeDisposable)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_RC1 -> {
                if (grantResults.any { it == PERMISSION_GRANTED }) {
                    toast("Permission is granted")
                    getLastLocation()
                } else {
                    toast("Please grant permission to get your location!")
                }
            }
            LOCATION_PERMISSION_RC2 -> {
                if (grantResults.firstOrNull() == PERMISSION_GRANTED) {
                    toast("Permission is granted")
                    getNearCity()
                } else {
                    toast("Please grant permission to get your location!")
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> return super.onOptionsItemSelected(item)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    companion object {
        private const val LOCATION_PERMISSION_RC1 = 1
        private const val LOCATION_PERMISSION_RC2 = 2
        private const val REQUEST_CHECK_SETTINGS = 3
        const val ACTION_CHANGED_LOCATION = "ACTION_CHANGED_LOCATION"
        const val SELECTED_CITY = "SELECTED_CITY"
    }
}
