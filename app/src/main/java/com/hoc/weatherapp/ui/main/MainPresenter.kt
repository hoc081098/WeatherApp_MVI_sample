package com.hoc.weatherapp.ui.main

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.ui.main.MainContract.ViewState.CityAndWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.NoSelectedCity
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap

class MainPresenter(
  currentWeatherRepository: CurrentWeatherRepository,
  private val colorHolderSource: ColorHolderSource
) : MviBasePresenter<MainContract.View, MainContract.ViewState>() {
  private var disposable: Disposable? = null
  private val colorCache = ConcurrentHashMap<Int, ColorHolderSource.Colors>()

  private val state = Observables.combineLatest(
    source1 = currentWeatherRepository.getSelectedCityAndCurrentWeatherOfSelectedCity(),
    source2 = colorHolderSource.colorObservable
  ) { weatherOptional, colorHolder ->
    when (weatherOptional) {
      None -> NoSelectedCity(colorHolderSource.defaultStatusBarColor)
      is Some -> CityAndWeather(
        city = weatherOptional.value.city,
        weather = weatherOptional.value.currentWeather,
        statusBarColor = colorHolder.statusBarColor
      )
    }
  }
    .distinctUntilChanged()
    .doOnNext { debug("ViewState=$it", TAG) }
    .observeOn(AndroidSchedulers.mainThread())!!

  override fun bindIntents() {
    disposable = intent(MainContract.View::changeColorIntent)
      .switchMap { optional ->
        when (optional) {
          is Some -> Observable.defer {
            val (bitmap, backgroundId) = optional.value
            colorCache[backgroundId]?.let {
              debug("Hit cache: backgroundId=$backgroundId, pair=$it", TAG)
              return@defer Observable.just(it)
            }

            Observable
              .fromCallable {
                getColors(
                  resource = bitmap,
                  defaultStatusBarColor = colorHolderSource.defaultStatusBarColor,
                  defaultBackgroundColor = colorHolderSource.defaultBackgroundColor,
                )
              }
              .doOnNext {
                colorCache[backgroundId] = it
                debug("Update cache: backgroundId=$backgroundId, pair=$it", TAG)
              }
              .subscribeOn(Schedulers.computation())
          }
          None -> Observable.empty()
        }
      }
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext { debug("ChangeColor=$it", TAG) }
      .subscribe(colorHolderSource::change)

    subscribeViewState(state, MainContract.View::render)
  }

  override fun unbindIntents() {
    super.unbindIntents()
    disposable?.dispose()
    colorCache.clear()
  }

  private companion object {
    private const val TAG = "__main__"
  }
}

@WorkerThread
private fun getColors(
  resource: Bitmap,
  @ColorInt defaultStatusBarColor: Int,
  @ColorInt defaultBackgroundColor: Int,
): ColorHolderSource.Colors {
  return Palette
    .from(resource)
    .generate()
    .let { palette ->
      @ColorInt val statusBarColor = listOf(
        palette.getSwatchForTarget(Target.DARK_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb,
        palette.getSwatchForTarget(Target.MUTED)?.rgb,
        palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb
      ).find { it !== null } ?: defaultStatusBarColor

      @ColorInt val backgroundColor = listOf(
        palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.LIGHT_MUTED)?.rgb
      ).find { it !== null } ?: defaultBackgroundColor

      ColorHolderSource.Colors(statusBarColor, backgroundColor)
    }
}
