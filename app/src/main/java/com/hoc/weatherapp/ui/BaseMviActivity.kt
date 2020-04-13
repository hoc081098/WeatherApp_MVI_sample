package com.hoc.weatherapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.hannesdorfmann.mosby3.mvi.MviPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.local.SettingPreferences
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

abstract class BaseMviActivity<V : MvpView, P : MviPresenter<V, *>> : MviActivity<V, P>() {
  private val settings by inject<SettingPreferences>()
  private val compositeDisposable = CompositeDisposable()

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(settings.darkThemePreference.value)
    super.onCreate(savedInstanceState)
    observeTheme(settings.darkThemePreference.observable).addTo(compositeDisposable)
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }
}

abstract class BaseAppCompatActivity : AppCompatActivity() {
  private val settings by inject<SettingPreferences>()
  private val compositeDisposable = CompositeDisposable()

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(settings.darkThemePreference.value)
    super.onCreate(savedInstanceState)
    observeTheme(settings.darkThemePreference.observable).addTo(compositeDisposable)
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }
}

@CheckResult
private fun AppCompatActivity.observeTheme(darkThemeObservable: Observable<Boolean>): Disposable {
  return darkThemeObservable
      .distinctUntilChanged()
      .skip(1)
      .delay(50, TimeUnit.MILLISECONDS)
      .subscribeBy {
        Intent(this, this::class.java)
            .let(::startActivity)
        finish()
      }
}

private fun AppCompatActivity.setTheme(isDarkTheme: Boolean) {
  setTheme(
      if (isDarkTheme) {
        R.style.AppTheme_DarkTheme_NoActionBar
      } else {
        R.style.AppTheme_LightTheme_NoActionBar
      }
  )
}