package com.hoc.weatherapp.utils.ui

import android.os.Looper
import androidx.annotation.CheckResult
import com.miguelcatalan.materialsearchview.MaterialSearchView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.disposables.Disposables

fun checkMainThread(observer: Observer<*>): Boolean {
  if (Looper.myLooper() != Looper.getMainLooper()) {
    observer.onSubscribe(Disposables.empty())
    observer.onError(
      IllegalStateException(
        "Expected to be called on the main thread but was ${Thread.currentThread().name}"
      )
    )
    return false
  }
  return true
}

@CheckResult
fun MaterialSearchView.textChanges(): Observable<String> {
  return MaterialSearchViewObservable(this)
}

internal class MaterialSearchViewObservable(private val view: MaterialSearchView) :
  Observable<String>() {
  override fun subscribeActual(observer: Observer<in String>) {
    if (!checkMainThread(observer)) {
      return
    }
    Listener(view, observer).let { listener ->
      observer.onSubscribe(listener)
      view.setOnQueryTextListener(listener)
    }
  }

  private class Listener(
    private val view: MaterialSearchView,
    private val observer: Observer<in String>
  ) :
    MainThreadDisposable(), MaterialSearchView.OnQueryTextListener {
    override fun onQueryTextChange(newText: String?): Boolean {
      return newText?.let {
        if (!isDisposed) {
          observer.onNext(it)
        }
        true
      } == true
    }

    override fun onQueryTextSubmit(query: String?) = true

    override fun onDispose() = view.setOnQueryTextListener(null)
  }
}
