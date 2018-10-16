package com.hoc.weatherapp.utils

import android.os.Looper
import androidx.annotation.CheckResult
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.disposables.Disposables

fun checkMainThread(observer: Observer<*>): Boolean {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        observer.onSubscribe(Disposables.empty())
        observer.onError(
            IllegalStateException(

                "Expected to be called on the main thread but was " + Thread.currentThread().name
            )
        )
        return false
    }
    return true
}

@CheckResult
fun SwipeRefreshLayout.refreshes(): Observable<Unit> {
    return SwipeRefreshLayoutRefreshObservable(this)
}

internal class SwipeRefreshLayoutRefreshObservable(private val view: SwipeRefreshLayout) :
    Observable<Unit>() {

    override fun subscribeActual(observer: Observer<in Unit>) {
        if (!checkMainThread(observer)) {
            return
        }
        Listener(view, observer).let { listener ->
            observer.onSubscribe(listener)
            view.setOnRefreshListener(listener)
        }
    }

    internal class Listener(
        private var view: SwipeRefreshLayout,
        private val observer: Observer<in Unit>
    ) : MainThreadDisposable(), OnRefreshListener {

        override fun onRefresh() {
            if (!isDisposed) {
                observer.onNext(Unit)
            }
        }

        override fun onDispose() {
            view.setOnRefreshListener(null)
        }
    }
}