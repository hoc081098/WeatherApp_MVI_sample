package com.hoc.weatherapp.koin

import com.hoc.weatherapp.data.CityRepository
import com.hoc.weatherapp.data.CityRepositoryImpl
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.CurrentWeatherRepositoryImpl
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.FiveDayForecastRepositoryImpl
import com.hoc.weatherapp.data.local.AppDatabase
import com.hoc.weatherapp.data.local.CityDao
import com.hoc.weatherapp.data.local.CityLocalDataSource
import com.hoc.weatherapp.data.local.CurrentWeatherDao
import com.hoc.weatherapp.data.local.CurrentWeatherLocalDataSource
import com.hoc.weatherapp.data.local.FiveDayForecastDao
import com.hoc.weatherapp.data.local.FiveDayForecastLocalDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

val dataSourceModule = module {
  single { getFiveDayForecastRepositoryImpl() } bind FiveDayForecastRepository::class

  single { getCityRepositoryImpl() } bind CityRepository::class

  single { getCurrentWeatherRepositoryImpl() } bind CurrentWeatherRepository::class

  single { getAppDatabase() }

  single { getCurrentWeatherDao() }

  single { getFiveDayForecastDao() }

  single { getCityDao() }

  single { getFiveDayForecastLocalDataSource() }

  single { getCurrentWeatherLocalDataSource() }

  single { getCityLocalDataSource() }
}

private fun Scope.getCityLocalDataSource(): CityLocalDataSource {
  return CityLocalDataSource(get())
}

private fun Scope.getCurrentWeatherLocalDataSource(): CurrentWeatherLocalDataSource {
  return CurrentWeatherLocalDataSource(get())
}

private fun Scope.getFiveDayForecastLocalDataSource(): FiveDayForecastLocalDataSource {
  return FiveDayForecastLocalDataSource(get())
}

private fun Scope.getCityDao(): CityDao {
  return get<AppDatabase>().cityDao()
}

private fun Scope.getFiveDayForecastDao(): FiveDayForecastDao {
  return get<AppDatabase>().fiveDayForecastDao()
}

private fun Scope.getCurrentWeatherDao(): CurrentWeatherDao {
  return get<AppDatabase>().weatherDao()
}

private fun Scope.getAppDatabase(): AppDatabase {
  return AppDatabase.getInstance(androidContext())
}

private fun Scope.getCurrentWeatherRepositoryImpl(): CurrentWeatherRepositoryImpl {
  return CurrentWeatherRepositoryImpl(get(), get(), get(), get(), get(), get())
}

private fun Scope.getCityRepositoryImpl(): CityRepositoryImpl {
  return CityRepositoryImpl(get(), get(), get(), get(), get(), get())
}

private fun Scope.getFiveDayForecastRepositoryImpl(): FiveDayForecastRepositoryImpl {
  return FiveDayForecastRepositoryImpl(get(), get(), get())
}
