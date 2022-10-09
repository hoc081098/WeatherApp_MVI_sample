package com.hoc.weatherapp.utils

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
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

@Suppress("ControlFlowWithEmptyBody", "ControlFlowWithEmptyBody")
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

@Suppress("ControlFlowWithEmptyBody")
inline fun <reified T : Any> T.debug(msg: Any?, tag: String? = null, throwable: Throwable? = null) {
  if (BuildConfig.DEBUG) {
    Log.d(tag ?: this::class.java.simpleName, msg.toString(), throwable)
  } else {
    // Not logging in release mode
  }
}

object AccessLocationPermissionDeniedException :
  Exception("need granted access location permission")

@SuppressLint("MissingPermission")
fun Context.checkLocationSettingAndGetCurrentLocation(
  settingsClient: SettingsClient,
  locationSettingsRequest: LocationSettingsRequest,
  fusedLocationProviderClient: FusedLocationProviderClient,
  locationRequest: LocationRequest
): Single<Location> {
  @Suppress("ControlFlowWithEmptyBody")
  return Observable
    .create<Location> { emitter ->
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
            override fun onLocationResult(locationResult: LocationResult) {
              val lastLocation = locationResult.lastLocation ?: return

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
  if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) != PERMISSION_GRANTED
  ) {
    if (!emitter.isDisposed) {
      emitter.onError(AccessLocationPermissionDeniedException)
    }
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

fun Context.themeColor(@AttrRes attrRes: Int): Int {
  val typedValue = TypedValue()
  theme.resolveAttribute(attrRes, typedValue, true)
  return typedValue.data
}

fun Drawable.setColorFilter(color: Int, mode: Mode = Mode.SRC_ATOP) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    colorFilter = BlendModeColorFilter(color, mode.getBlendMode())
  } else {
    @Suppress("DEPRECATION")
    setColorFilter(color, mode.getPorterDuffMode())
  }
}

// This class is needed to call the setColorFilter
// with different BlendMode on older API (before 29).
enum class Mode {
  CLEAR,
  SRC,
  DST,
  SRC_OVER,
  DST_OVER,
  SRC_IN,
  DST_IN,
  SRC_OUT,
  DST_OUT,
  SRC_ATOP,
  DST_ATOP,
  XOR,
  DARKEN,
  LIGHTEN,
  MULTIPLY,
  SCREEN,
  ADD,
  OVERLAY;

  @RequiresApi(Build.VERSION_CODES.Q)
  fun getBlendMode(): BlendMode =
    when (this) {
      CLEAR -> BlendMode.CLEAR
      SRC -> BlendMode.SRC
      DST -> BlendMode.DST
      SRC_OVER -> BlendMode.SRC_OVER
      DST_OVER -> BlendMode.DST_OVER
      SRC_IN -> BlendMode.SRC_IN
      DST_IN -> BlendMode.DST_IN
      SRC_OUT -> BlendMode.SRC_OUT
      DST_OUT -> BlendMode.DST_OUT
      SRC_ATOP -> BlendMode.SRC_ATOP
      DST_ATOP -> BlendMode.DST_ATOP
      XOR -> BlendMode.XOR
      DARKEN -> BlendMode.DARKEN
      LIGHTEN -> BlendMode.LIGHTEN
      MULTIPLY -> BlendMode.MULTIPLY
      SCREEN -> BlendMode.SCREEN
      ADD -> BlendMode.PLUS
      OVERLAY -> BlendMode.OVERLAY
    }

  fun getPorterDuffMode(): PorterDuff.Mode =
    when (this) {
      CLEAR -> PorterDuff.Mode.CLEAR
      SRC -> PorterDuff.Mode.SRC
      DST -> PorterDuff.Mode.DST
      SRC_OVER -> PorterDuff.Mode.SRC_OVER
      DST_OVER -> PorterDuff.Mode.DST_OVER
      SRC_IN -> PorterDuff.Mode.SRC_IN
      DST_IN -> PorterDuff.Mode.DST_IN
      SRC_OUT -> PorterDuff.Mode.SRC_OUT
      DST_OUT -> PorterDuff.Mode.DST_OUT
      SRC_ATOP -> PorterDuff.Mode.SRC_ATOP
      DST_ATOP -> PorterDuff.Mode.DST_ATOP
      XOR -> PorterDuff.Mode.XOR
      DARKEN -> PorterDuff.Mode.DARKEN
      LIGHTEN -> PorterDuff.Mode.LIGHTEN
      MULTIPLY -> PorterDuff.Mode.MULTIPLY
      SCREEN -> PorterDuff.Mode.SCREEN
      ADD -> PorterDuff.Mode.ADD
      OVERLAY -> PorterDuff.Mode.OVERLAY
    }
}
