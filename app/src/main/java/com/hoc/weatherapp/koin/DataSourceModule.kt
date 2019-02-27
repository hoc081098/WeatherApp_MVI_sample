package com.hoc.weatherapp.koin

import com.hoc.weatherapp.data.*
import com.hoc.weatherapp.data.local.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.module

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

private fun ModuleDefinition.getCityLocalDataSource(): CityLocalDataSource {
  return CityLocalDataSource(get())
}

private fun ModuleDefinition.getCurrentWeatherLocalDataSource(): CurrentWeatherLocalDataSource {
  return CurrentWeatherLocalDataSource(get())
}

private fun ModuleDefinition.getFiveDayForecastLocalDataSource(): FiveDayForecastLocalDataSource {
  return FiveDayForecastLocalDataSource(get())
}

private fun ModuleDefinition.getCityDao(): CityDao {
  return get<AppDatabase>().cityDao()
}

private fun ModuleDefinition.getFiveDayForecastDao(): FiveDayForecastDao {
  return get<AppDatabase>().fiveDayForecastDao()
}

private fun ModuleDefinition.getCurrentWeatherDao(): CurrentWeatherDao {
  return get<AppDatabase>().weatherDao()
}

private fun ModuleDefinition.getAppDatabase(): AppDatabase {
  return AppDatabase.getInstance(androidContext())
}

private fun ModuleDefinition.getCurrentWeatherRepositoryImpl(): CurrentWeatherRepositoryImpl {
  return CurrentWeatherRepositoryImpl(get(), get(), get(), get(), get(), get())
}

private fun ModuleDefinition.getCityRepositoryImpl(): CityRepositoryImpl {
  return CityRepositoryImpl(get(), get(), get(), get(), get(), get())
}

private fun ModuleDefinition.getFiveDayForecastRepositoryImpl(): FiveDayForecastRepositoryImpl {
  return FiveDayForecastRepositoryImpl(get(), get(), get())
}