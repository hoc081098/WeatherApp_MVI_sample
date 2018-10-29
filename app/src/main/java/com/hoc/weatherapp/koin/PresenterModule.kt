package com.hoc.weatherapp.koin

import com.hoc.weatherapp.ui.addcity.AddCityPresenter
import com.hoc.weatherapp.ui.cities.CitiesPresenter
import com.hoc.weatherapp.ui.main.MainPresenter
import com.hoc.weatherapp.ui.main.chart.ChartPresenter
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherPresenter
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherPresenter
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module

val presenterModule = module {
  factory { CitiesPresenter(get(), get()) }

  factory { CurrentWeatherPresenter(get()) }

  factory { AddCityPresenter(get(), androidApplication()) }

  factory { DailyWeatherPresenter(get(), get()) }

  factory { MainPresenter(get(), get(), androidApplication()) }

  factory { ChartPresenter(get(), get()) }
}