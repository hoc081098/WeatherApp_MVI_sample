package com.hoc.weatherapp.koin

import com.hoc.weatherapp.ui.addcity.AddCityPresenter
import com.hoc.weatherapp.ui.cities.CitiesPresenter
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module

val presenterModule = module {
  factory { CitiesPresenter(get(), get()) }

  factory { AddCityPresenter(get(), androidApplication()) }
}