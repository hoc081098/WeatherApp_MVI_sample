package com.hoc.weatherapp.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter

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

fun View.snackBar(message: CharSequence, @DrawableRes duration: Int = Snackbar.LENGTH_SHORT) =
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