package com.hoc.weatherapp.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.Single

@IntDef(value = [Toast.LENGTH_SHORT, Toast.LENGTH_LONG])
@Retention(AnnotationRetention.SOURCE)
annotation class ToastDuration

@IntDef(value = [Snackbar.LENGTH_SHORT, Snackbar.LENGTH_INDEFINITE, Snackbar.LENGTH_LONG])
@Retention(AnnotationRetention.SOURCE)
annotation class SnackbarDuration

fun Context.toast(message: CharSequence, @ToastDuration duration: Int = Toast.LENGTH_SHORT): Toast =
  Toast.makeText(
    this,
    message,
    duration
  ).apply(Toast::show)

fun View.snackBar(message: CharSequence, @SnackbarDuration duration: Int = Snackbar.LENGTH_SHORT) =
  Snackbar.make(
    this,
    message,
    duration
  ).apply(Snackbar::show)

fun Fragment.toast(message: CharSequence) = context?.toast(message)

inline fun <reified T : Context> Context.startActivity() =
  startActivity(Intent(this, T::class.java))

inline fun <reified T : Any> T.debug(msg: Any?, tag: String? = null) {
  Log.d(tag ?: this::class.java.simpleName, msg.toString())
}

fun Context.checkLocationSettingAndGetCurrentLocation(): Single<Location> {
  return Observable.create<Location> { emitter ->
    val locationRequest = LocationRequest()
      .setInterval(500)
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
          emitter.onError(IllegalStateException("Need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        fusedLocationProviderClient.lastLocation
          .addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
              if (!emitter.isDisposed) {
                emitter.onNext(lastLocation)
                emitter.onComplete()
              }
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
          emitter.onError(IllegalStateException("need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        val callback = object : LocationCallback() {
          override fun onLocationResult(locationResult: LocationResult?) {
            val lastLocation = locationResult?.lastLocation ?: return
            if (!emitter.isDisposed) {
              emitter.onNext(lastLocation)
              emitter.onComplete()
              debug(
                "Get location successfully: $lastLocation",
                "Context::checkLocationSettingAndGetCurrentLocation"
              )
            }
          }
        }
        fusedLocationProviderClient.requestLocationUpdates(
          locationRequest,
          callback,
          null /* LOOPER */
        )

        emitter.setCancellable {
          debug("removeLocationUpdates", "Context::checkLocationSettingAndGetCurrentLocation")
          fusedLocationProviderClient.removeLocationUpdates(callback)
        }
      }.addOnFailureListener(emitter::onError)
  }.take(1).singleOrError()
}

inline fun <reified R : Any> Observable<*>.notOfType(): Observable<out Any> =
  filter { !R::class.java.isInstance(it) }
