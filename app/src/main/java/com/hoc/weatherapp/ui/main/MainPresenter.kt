package com.hoc.weatherapp.ui.main

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.ui.main.MainContract.ViewState.CityAndWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.NoSelectedCity
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.themeColor
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap

class MainPresenter(
  currentWeatherRepository: CurrentWeatherRepository,
  private val colorHolderSource: ColorHolderSource,
  private val androidApplication: Application
) : MviBasePresenter<MainContract.View, MainContract.ViewState>() {
  private var disposable: Disposable? = null
  private val colorCache = ConcurrentHashMap<Int, Pair<Int, Int>>()

  private val state = Observables.combineLatest(
    source1 = currentWeatherRepository.getSelectedCityAndCurrentWeatherOfSelectedCity(),
    source2 = colorHolderSource.colorObservable
  ).map {
    when (val optional = it.first) {
      None -> NoSelectedCity(androidApplication.themeColor(R.attr.colorPrimaryVariant))
      is Some -> CityAndWeather(
        city = optional.value.city,
        weather = optional.value.currentWeather,
        vibrantColor = it.second.first
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
                getVibrantColor(
                  resource = bitmap,
                  colorPrimaryVariant = colorHolderSource.defaultColorPrimaryVariant,
                  colorSecondary = colorHolderSource.defaultColorSecondary,
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
private fun getVibrantColor(
  resource: Bitmap,
  @ColorInt colorPrimaryVariant: Int,
  @ColorInt colorSecondary: Int,
): Pair<Int, Int> {
  return Palette
    .from(resource)
    .generate()
    .let { palette ->
      @ColorInt val darkColor = listOf(
        palette.getSwatchForTarget(Target.DARK_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb,
        palette.getSwatchForTarget(Target.MUTED)?.rgb,
        palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb
      ).find { it !== null } ?: colorPrimaryVariant

      @ColorInt val lightColor = listOf(
        palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.LIGHT_MUTED)?.rgb
      ).find { it !== null } ?: colorSecondary

      darkColor to lightColor
    }
}
