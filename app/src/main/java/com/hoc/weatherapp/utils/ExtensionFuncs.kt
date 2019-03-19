package com.hoc.weatherapp.utils

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.material.snackbar.Snackbar
import com.hoc.weatherapp.BuildConfig
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.subjects.Subject

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

inline fun <reified T : Any> T.debug(msg: Any?, tag: String? = null, throwable: Throwable? = null) {
  if (BuildConfig.DEBUG) {
    Log.d(tag ?: this::class.java.simpleName, msg.toString(), throwable)
  } else {
    //Not logging in release mode
  }
}

@SuppressLint("MissingPermission")
fun Context.checkLocationSettingAndGetCurrentLocation(
  settingsClient: SettingsClient,
  locationSettingsRequest: LocationSettingsRequest,
  fusedLocationProviderClient: FusedLocationProviderClient,
  locationRequest: LocationRequest
): Single<Location> {
  return Observable.create<Location> { emitter ->
    settingsClient
      .checkLocationSettings(locationSettingsRequest)
      .addOnFailureListener {
        if (!emitter.isDisposed) {
          emitter.onError(it)
        }
      }
      .addOnSuccessListener {

        if (isAccessLocationPermissionDenied(emitter)) return@addOnSuccessListener
        fusedLocationProviderClient
          .lastLocation
          .addOnSuccessListener { lastLocation ->
            if (lastLocation != null && !emitter.isDisposed) {
              emitter.onNext(lastLocation)
              emitter.onComplete()
            }
          }

        if (isAccessLocationPermissionDenied(emitter)) return@addOnSuccessListener
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
          debug(
            "removeLocationUpdates",
            "Context::checkLocationSettingAndGetCurrentLocation"
          )
          fusedLocationProviderClient.removeLocationUpdates(callback)
        }
      }
  }
    .take(1)
    .singleOrError()
}

private fun Context.isAccessLocationPermissionDenied(emitter: ObservableEmitter<Location>): Boolean {
  if (ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED
  ) {
    emitter.onError(IllegalStateException("need granted access location permission"))
    return true
  }
  return false
}

inline fun <reified R : Any> Observable<*>.notOfType(): Observable<out Any> =
  filter { !R::class.java.isInstance(it) }

inline fun <T, R> Observable<T>.exhaustMap(crossinline transform: (T) -> Observable<R>): Observable<R> {
  return this
    .toFlowable(BackpressureStrategy.DROP)
    .flatMap({ transform(it).toFlowable(BackpressureStrategy.MISSING) }, 1)
    .toObservable()
}

@Suppress("nothing_to_inline")
inline fun <T : Any> Subject<T>.asObservable(): Observable<T> = this


