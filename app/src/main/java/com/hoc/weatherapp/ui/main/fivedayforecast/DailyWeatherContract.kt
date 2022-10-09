package com.hoc.weatherapp.ui.main.fivedayforecast

import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface DailyWeatherContract {
  sealed class PartialStateChange {
    data class Error(
      val throwable: Throwable,
      val showMessage: Boolean
    ) : PartialStateChange()

    data class Weather(val dailyWeatherListItem: List<DailyWeatherListItem>) : PartialStateChange()

    data class RefreshWeatherSuccess(val showMessage: Boolean) : PartialStateChange()
  }

  data class ViewState(
    val dailyWeatherListItem: List<DailyWeatherListItem>? = null,
    val error: Throwable? = null,
    val showError: Boolean = false,
    val showRefreshSuccessfully: Boolean = false
  )

  sealed class RefreshIntent {
    object InitialRefreshIntent : RefreshIntent()
    object UserRefreshIntent : RefreshIntent()
  }

  interface View : MvpView {
    fun refreshDailyWeatherIntent(): Observable<RefreshIntent>

    fun render(viewState: ViewState)
  }
}
