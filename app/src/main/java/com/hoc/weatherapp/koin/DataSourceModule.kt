package com.hoc.weatherapp.koin

import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.RepositoryImpl
import com.hoc.weatherapp.data.local.AppDatabase
import com.hoc.weatherapp.data.local.CityLocalDataSource
import com.hoc.weatherapp.data.local.CurrentWeatherLocalDataSource
import com.hoc.weatherapp.data.local.DailyWeatherLocalDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val dataSourceModule = module {
  //    single { WeatherRepositoryImpl(get(), get()) }
//
//    single {
//        WeatherRepositoryImpl2(
//            get(),
//            get(),
//            get()
//        )
//    } bind WeatherRepository::class

  single { RepositoryImpl(get(), get(), get(), get(), get()) } bind Repository::class

  single { AppDatabase.getInstance(androidContext()) }

  single { get<AppDatabase>().weatherDao() }

  single { get<AppDatabase>().dailyWeatherDao() }

  single { get<AppDatabase>().cityDao() }

  single { DailyWeatherLocalDataSource(get()) }

  single { CurrentWeatherLocalDataSource(get()) }

  single { CityLocalDataSource(get()) }
}