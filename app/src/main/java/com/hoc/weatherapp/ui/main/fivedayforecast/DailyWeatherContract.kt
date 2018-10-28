package com.hoc.weatherapp.ui.main.fivedayforecast

import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface DailyWeatherContract {
  class ViewState(
    val dailyWeatherListItem: List<DailyWeatherListItem>
  )

  sealed class RefreshIntent {
    object InitialIntent : RefreshIntent()
    object UserIntent : RefreshIntent()
  }

  interface View : MvpView {
    fun refreshDailyWeatherIntent(): Observable<RefreshIntent>

    fun render(viewState: ViewState)
  }
}