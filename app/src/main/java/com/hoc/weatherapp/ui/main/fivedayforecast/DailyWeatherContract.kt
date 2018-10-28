package com.hoc.weatherapp.ui.main.fivedayforecast

import com.hannesdorfmann.mosby3.mvp.MvpView

interface DailyWeatherContract {
  class ViewState(
    val dailyWeatherListItem: List<DailyWeatherListItem>
  )

  interface View : MvpView {
    fun render(viewState: ViewState)
  }
}