package com.hoc.weatherapp.koin

import android.preference.PreferenceManager
import com.hoc.weatherapp.data.local.SharedPrefUtil
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module

val sharePrefUtilModule = module {
  single { PreferenceManager.getDefaultSharedPreferences(androidApplication()) }

  single { SharedPrefUtil(get(), androidApplication(), get()) }
}