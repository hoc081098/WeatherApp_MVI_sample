package com.hoc.weatherapp.koin

import android.preference.PreferenceManager
import com.hoc.weatherapp.data.local.SharedPrefUtil
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val sharePrefUtilModule = module {
  single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }

  single { SharedPrefUtil(get(), androidContext(), get()) }
}