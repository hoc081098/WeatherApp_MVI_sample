package com.hoc.weatherapp.koin

import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.data.WeatherRepositoryImpl
import com.hoc.weatherapp.data.WeatherRepositoryImpl2
import com.hoc.weatherapp.data.local.AppDatabase
import com.hoc.weatherapp.data.local.LocalDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val dataSourceModule = module {
    single { WeatherRepositoryImpl(get(), get()) }

    single { WeatherRepositoryImpl2(get(), get(), get()) } bind WeatherRepository::class

    single { AppDatabase.getInstance(androidContext()) }

    single { get<AppDatabase>().weatherDao() }

    single { get<AppDatabase>().dailyWeatherDao() }

    single { LocalDataSource(get(), get()) }
}