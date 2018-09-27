package com.hoc.weatherapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.miguelcatalan.materialsearchview.MaterialSearchView
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.BehaviorProcessor

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

fun MaterialSearchView.textChange(): Flowable<String> {
    val processor = BehaviorProcessor.create<String>()
    setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?) = true

        override fun onQueryTextChange(newText: String?) =
            newText?.let {
                processor.onNext(it)
                true
            } == true
    })
    return processor.onBackpressureLatest().hide()
}