package com.hoc.weatherapp.ui

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.hannesdorfmann.mosby3.mvi.MviPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject

@ExperimentalStdlibApi
abstract class BaseMviActivity<V : MvpView, P : MviPresenter<V, *>>(
  @LayoutRes private val contentLayoutId: Int,
  private val noActionBar: Boolean = true,
) : MviActivity<V, P>() {
  private val settings by inject<SettingPreferences>()
  private val compositeDisposable = CompositeDisposable()

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(settings.darkThemePreference.value, noActionBar)

    super.onCreate(savedInstanceState)
    setContentView(contentLayoutId)

    observeTheme(settings.darkThemePreference.observable).addTo(compositeDisposable)
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }
}

@ExperimentalStdlibApi
abstract class BaseAppCompatActivity(
  @LayoutRes contentLayoutId: Int,
  private val noActionBar: Boolean = true
) : AppCompatActivity(contentLayoutId) {
  private val settings by inject<SettingPreferences>()
  private val compositeDisposable = CompositeDisposable()

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(settings.darkThemePreference.value, noActionBar)

    super.onCreate(savedInstanceState)

    observeTheme(settings.darkThemePreference.observable).addTo(compositeDisposable)
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }
}

@ExperimentalStdlibApi
@CheckResult
private fun AppCompatActivity.observeTheme(darkThemeObservable: Observable<Boolean>): Disposable =
  darkThemeObservable
    .distinctUntilChanged()
    .skip(1)
    .take(1)
    .subscribeBy {
      overridePendingTransition(0, 0)
      finish()

      overridePendingTransition(0, 0)
      startActivity(intent)

      debug("Change theme $this $it", tag = "change_theme")
    }

private fun AppCompatActivity.setTheme(isDarkTheme: Boolean, noActionBar: Boolean) {
  setTheme(
    when {
      isDarkTheme -> {
        if (noActionBar) R.style.AppTheme_DarkTheme_NoActionBar
        else R.style.AppTheme_DarkTheme
      }
      else -> {
        if (noActionBar) R.style.AppTheme_LightTheme_NoActionBar
        else R.style.AppTheme_LightTheme
      }
    }
  )
}
