package com.hoc.weatherapp.ui.main

import android.app.Application
import androidx.core.content.ContextCompat
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.ui.main.MainContract.ViewState.CityAndWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.NoSelectedCity
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables

class MainPresenter(
  currentWeatherRepository: CurrentWeatherRepository,
  private val colorHolderSource: ColorHolderSource,
  private val androidApplication: Application
) : MviBasePresenter<MainContract.View, MainContract.ViewState>() {
  private var disposable: Disposable? = null

  private val state = Observables.combineLatest(
    source1 = currentWeatherRepository.getSelectedCityAndCurrentWeatherOfSelectedCity(),
    source2 = colorHolderSource.colorObservable
  ).map {
    when (val optional = it.first) {
      None -> NoSelectedCity(
        ContextCompat.getColor(
          androidApplication,
          R.color.colorPrimaryDark
        )
      )
      is Some -> CityAndWeather(
        city = optional.value.city,
        weather = optional.value.currentWeather,
        vibrantColor = it.second
      )
    }
  }
    .distinctUntilChanged()
    .doOnNext { debug("ViewState=$it", TAG) }
    .observeOn(AndroidSchedulers.mainThread())!!

  override fun bindIntents() {
    disposable = intent(MainContract.View::changeColorIntent)
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext { debug("ChangeColor=$it", TAG) }
      .subscribe(colorHolderSource::change)

    subscribeViewState(state, MainContract.View::render)
  }

  override fun unbindIntents() {
    super.unbindIntents()
    disposable?.takeUnless { it.isDisposed }?.dispose()
  }

  private companion object {
    private const val TAG = "__main__"
  }
}
