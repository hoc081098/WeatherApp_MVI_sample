package com.hoc.weatherapp.koin

import android.preference.PreferenceManager
import com.hoc.weatherapp.SharedPrefUtil
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val sharePrefUtilModule = module {
    single {
        SharedPrefUtil(PreferenceManager.getDefaultSharedPreferences(androidContext()), get())
    }
}