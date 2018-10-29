package com.hoc.weatherapp.ui.main.chart

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.entity.DailyWeather

interface ChartContract {
  data class ViewState(
    val weathers: List<DailyWeather>? = null,
    val temperatureUnit: TemperatureUnit
  )

  interface View : MvpView {
    fun render(viewState: ViewState)
  }

}