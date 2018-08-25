package com.hoc.weatherapp.koin

import com.hoc.weatherapp.data.AppDatabase
import com.hoc.weatherapp.data.LocalDataSource
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.data.WeatherRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val dataSourceModule = module {
    single { WeatherRepositoryImpl(get(), get()) } bind WeatherRepository::class

    single { AppDatabase.getInstance(androidContext()) }

    single { get<AppDatabase>().weatherDao() }

    single { LocalDataSource(get()) }
}