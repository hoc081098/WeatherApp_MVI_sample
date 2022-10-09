package com.hoc.weatherapp.ui.main.currentweather

import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface CurrentWeatherContract {
  sealed class PartialStateChange {
    data class Error(
      val throwable: Throwable,
      val showMessage: Boolean
    ) : PartialStateChange()

    data class Weather(val weather: CurrentWeather) : PartialStateChange()

    data class RefreshWeatherSuccess(val showMessage: Boolean) : PartialStateChange()
  }

  data class ViewState(
    val weather: CurrentWeather? = null,
    val error: Throwable? = null,
    val showError: Boolean = false,
    val showRefreshSuccessfully: Boolean = false
  )

  sealed class RefreshIntent {
    object InitialRefreshIntent : RefreshIntent()
    object UserRefreshIntent : RefreshIntent()
  }

  interface View : MvpView {
    fun refreshCurrentWeatherIntent(): Observable<RefreshIntent>

    fun render(state: ViewState)
  }
}
