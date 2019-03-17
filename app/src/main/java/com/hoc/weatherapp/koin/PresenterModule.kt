package com.hoc.weatherapp.koin

import com.hoc.weatherapp.ui.addcity.AddCityPresenter
import com.hoc.weatherapp.ui.cities.CitiesPresenter
import com.hoc.weatherapp.ui.main.ColorHolderSource
import com.hoc.weatherapp.ui.main.MainPresenter
import com.hoc.weatherapp.ui.main.chart.ChartPresenter
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherPresenter
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherPresenter
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.module

val presenterModule = module {
  factory { getCitiesPresenter() }

  factory { getCurrentWeatherPresenter() }

  factory { getAddCityPresenter() }

  factory { getDailyWeatherPresenter() }

  factory { getMainPresenter() }

  factory { getChartPresenter() }

  single { getColorHolderSource() }
}

private fun ModuleDefinition.getColorHolderSource() = ColorHolderSource(androidApplication())

private fun ModuleDefinition.getChartPresenter(): ChartPresenter {
  return ChartPresenter(get(), get())
}

private fun ModuleDefinition.getMainPresenter(): MainPresenter {
  return MainPresenter(get(), get(), androidApplication())
}

private fun ModuleDefinition.getDailyWeatherPresenter(): DailyWeatherPresenter {
  return DailyWeatherPresenter(get(), get(), get(), get(), androidApplication())
}

private fun ModuleDefinition.getAddCityPresenter(): AddCityPresenter {
  return AddCityPresenter(get(), get(), androidApplication())
}

private fun ModuleDefinition.getCurrentWeatherPresenter(): CurrentWeatherPresenter {
  return CurrentWeatherPresenter(get(), get(), androidApplication(), get())
}

private fun ModuleDefinition.getCitiesPresenter(): CitiesPresenter {
  return CitiesPresenter(get(), get(), get(), get(), androidApplication())
}