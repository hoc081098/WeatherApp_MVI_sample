package com.hoc.weatherapp.ui.main.chart

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.ui.main.chart.ChartContract.View
import com.hoc.weatherapp.ui.main.chart.ChartContract.ViewState
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables.combineLatest

class ChartPresenter(
  fiveDayForecastRepository: FiveDayForecastRepository,
  settingPreferences: SettingPreferences
) : MviBasePresenter<View, ViewState>() {
  private val viewState = combineLatest(
    source1 = fiveDayForecastRepository.getFiveDayForecastOfSelectedCity(),
    source2 = settingPreferences.temperatureUnitPreference.observable,
    source3 = settingPreferences.speedUnitPreference.observable,
    source4 = settingPreferences.pressureUnitPreference.observable,
    combineFunction = { optional, temperatureUnit, speedUnit, pressureUnit ->
      Tuple4(
        temperatureUnit = temperatureUnit,
        weathers = optional.getOrNull()?.second.orEmpty(),
        pressureUnit = pressureUnit,
        speedUnit = speedUnit
      )
    }
  )
    .map {
      ViewState(
        temperatureUnit = it.temperatureUnit,
        weathers = it.weathers,
        speedUnit = it.speedUnit,
        pressureUnit = it.pressureUnit
      )
    }
    .distinctUntilChanged()
    .doOnNext { debug("ViewState=$it", TAG) }
    .observeOn(AndroidSchedulers.mainThread())!!

  override fun bindIntents() = subscribeViewState(viewState, View::render)

  private companion object {
    private const val TAG = "__chart__"

    private data class Tuple4(
      val weathers: List<DailyWeather>,
      val temperatureUnit: TemperatureUnit,
      val speedUnit: SpeedUnit,
      val pressureUnit: PressureUnit
    )
  }
}
