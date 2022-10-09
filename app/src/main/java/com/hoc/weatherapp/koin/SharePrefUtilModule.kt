package com.hoc.weatherapp.koin

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.hoc.weatherapp.data.local.SelectedCityPreference
import com.hoc.weatherapp.data.local.SettingPreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.core.scope.Scope
import org.koin.dsl.module

val sharePrefUtilModule = module {
  single { getSharedPreferences() }

  single { getSettingPreference() }

  single(createdAtStart = true) { getCityPreference() }
}

private fun Scope.getCityPreference(): SelectedCityPreference {
  return SelectedCityPreference(get(), get())
}

private fun Scope.getSettingPreference(): SettingPreferences {
  return SettingPreferences(get(), androidApplication())
}

private fun Scope.getSharedPreferences(): SharedPreferences {
  return PreferenceManager.getDefaultSharedPreferences(androidApplication())
}
