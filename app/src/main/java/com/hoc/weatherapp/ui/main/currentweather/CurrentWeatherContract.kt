package com.hoc.weatherapp.ui.main.currentweather

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import io.reactivex.Observable

interface CurrentWeatherContract {
  sealed class PartialChange {
    data class Error(
      val throwable: Throwable,
      val showMessage: Boolean
    ) : PartialChange()

    data class Weather(val weather: CurrentWeather) : PartialChange()

    data class RefreshWeatherSuccess(
      val weather: CurrentWeather,
      val showMessage: Boolean
    ) : PartialChange()
  }

  data class ViewState(
    val weather: CurrentWeather? = null,
    val error: Throwable? = null,
    val showError: Boolean = false,
    val showRefreshSuccessfully: Boolean = false
  )

  interface View : MvpView {
    fun refreshCurrentWeatherIntent(): Observable<Unit>

    fun render(state: ViewState)
  }
}