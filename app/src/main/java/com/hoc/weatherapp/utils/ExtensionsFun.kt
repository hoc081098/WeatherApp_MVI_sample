package com.hoc.weatherapp.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers

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

fun <T : Any?> Task<T>.toFlowable(): Flowable<T> {
  return Flowable.create({ emitter: FlowableEmitter<T> ->
    addOnSuccessListener {
      if (!emitter.isCancelled) {
        if (it === null) {
          emitter.onError(NullPointerException("Result of task is null"))
        } else {
          emitter.onNext(it)
          emitter.onComplete()
        }
      }
    }.addOnFailureListener {
      if (!emitter.isCancelled) {
        emitter.onError(it)
      }
    }
  }, BackpressureStrategy.LATEST)
}

fun Context.receivesLocal(intentFilter: IntentFilter): Flowable<Intent> {
  return Flowable.create({ emitter: FlowableEmitter<Intent> ->
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent) {
        debug("onReceive $intent", this::class.java.simpleName)
        emitter.onNext(intent)
      }
    }

    LocalBroadcastManager.getInstance(this)
      .registerReceiver(receiver, intentFilter)
    debug(Looper.getMainLooper() == Looper.myLooper(), this::class.java.simpleName)

    emitter.setCancellable {
      if (Looper.getMainLooper() == Looper.myLooper()) {
        LocalBroadcastManager.getInstance(this)
          .unregisterReceiver(receiver)
        debug("unregisterReceiver1", this::class.java.simpleName)
      } else {
        val worker = AndroidSchedulers.mainThread().createWorker()
        worker.schedule {
          LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(receiver)
          debug("unregisterReceiver2", this::class.java.simpleName)
          worker.dispose()
        }
      }
    }
  }, BackpressureStrategy.LATEST)
}

fun Context.checkLocationSettingAndGetCurrentLocation(): Single<Location> {
  return Observable.generate<Location> { emitter ->
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
          emitter.onError(IllegalStateException("Need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        fusedLocationProviderClient.lastLocation
          .addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
              emitter.onNext(lastLocation)
              emitter.onComplete()
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
          emitter.onError(IllegalStateException("Need granted permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
          return@addOnSuccessListener
        }

        fusedLocationProviderClient.requestLocationUpdates(
          locationRequest,
          object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
              val lastLocation = locationResult?.lastLocation ?: return
              fusedLocationProviderClient.removeLocationUpdates(this)
              emitter.onNext(lastLocation)
              emitter.onComplete()
            }
          },
          null /* LOOPER */
        )
      }.addOnFailureListener(emitter::onError)
  }.take(1).singleOrError()
}


