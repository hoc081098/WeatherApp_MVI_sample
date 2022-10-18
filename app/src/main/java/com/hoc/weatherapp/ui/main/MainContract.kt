package com.hoc.weatherapp.ui.main

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.utils.Optional
import io.reactivex.Observable

interface MainContract {
  sealed class ViewState {
    @get:ColorInt
    abstract val statusBarColor: Int

    data class CityAndWeather(
      val city: City,
      val weather: CurrentWeather,
      @ColorInt override val statusBarColor: Int
    ) : ViewState()

    data class NoSelectedCity(@ColorInt override val statusBarColor: Int) : ViewState()
  }

  data class BitmapAndBackgroundId(
    val bitmap: Bitmap,
    @DrawableRes val backgroundId: Int
  )

  interface View : MvpView {
    fun changeColorIntent(): Observable<Optional<BitmapAndBackgroundId>>

    fun render(state: ViewState)
  }
}
