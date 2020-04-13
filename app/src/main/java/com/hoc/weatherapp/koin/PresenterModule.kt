package com.hoc.weatherapp.koin

import com.hoc.weatherapp.ui.addcity.AddCityPresenter
import com.hoc.weatherapp.ui.cities.CitiesPresenter
import com.hoc.weatherapp.ui.main.ColorHolderSource
import com.hoc.weatherapp.ui.main.MainPresenter
import com.hoc.weatherapp.ui.main.chart.ChartPresenter
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherPresenter
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherPresenter
import org.koin.android.ext.koin.androidApplication
import org.koin.core.scope.Scope
import org.koin.dsl.module

@ExperimentalStdlibApi val presenterModule = module {
  factory { getCitiesPresenter() }

  factory { getCurrentWeatherPresenter() }

  factory { getAddCityPresenter() }

  factory { getDailyWeatherPresenter() }

  factory { getMainPresenter() }

  factory { getChartPresenter() }

  single { getColorHolderSource() }
}

private fun Scope.getColorHolderSource() = ColorHolderSource(androidApplication())

private fun Scope.getChartPresenter(): ChartPresenter {
  return ChartPresenter(get(), get())
}

private fun Scope.getMainPresenter(): MainPresenter {
  return MainPresenter(get(), get(), androidApplication())
}

@ExperimentalStdlibApi
private fun Scope.getDailyWeatherPresenter(): DailyWeatherPresenter {
  return DailyWeatherPresenter(get(), get(), get(), get(), androidApplication())
}

private fun Scope.getAddCityPresenter(): AddCityPresenter {
  return AddCityPresenter(get(), get(), androidApplication())
}

@ExperimentalStdlibApi
private fun Scope.getCurrentWeatherPresenter(): CurrentWeatherPresenter {
  return CurrentWeatherPresenter(get(), get(), androidApplication(), get())
}

@ExperimentalStdlibApi
private fun Scope.getCitiesPresenter(): CitiesPresenter {
  return CitiesPresenter(get(), get(), get(), get(), androidApplication())
}