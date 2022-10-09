package com.hoc.weatherapp.ui.main.chart

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.entity.DailyWeather

interface ChartContract {
  data class ViewState(
    val weathers: List<DailyWeather>,
    val temperatureUnit: TemperatureUnit,
    val pressureUnit: PressureUnit,
    val speedUnit: SpeedUnit
  )

  interface View : MvpView {
    fun render(viewState: ViewState)
  }
}
