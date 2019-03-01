package com.hoc.weatherapp.koin

import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.hoc.weatherapp.data.local.SelectedCityPreference
import com.hoc.weatherapp.data.local.SettingPreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.module

val sharePrefUtilModule = module {
  single { getSharedPreferences() }

  single { getSettingPreference() }

  single(createOnStart = true) { getCityPreference() }
}

private fun ModuleDefinition.getCityPreference(): SelectedCityPreference {
  return SelectedCityPreference(get(), get())
}

private fun ModuleDefinition.getSettingPreference(): SettingPreferences {
  return SettingPreferences(get(), androidApplication())
}

private fun ModuleDefinition.getSharedPreferences(): SharedPreferences {
  return PreferenceManager.getDefaultSharedPreferences(androidApplication())!!
}