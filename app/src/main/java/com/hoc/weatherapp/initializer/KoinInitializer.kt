package com.hoc.weatherapp.initializer

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.hoc.weatherapp.BuildConfig
import com.hoc.weatherapp.koin.dataSourceModule
import com.hoc.weatherapp.koin.presenterModule
import com.hoc.weatherapp.koin.retrofitModule
import com.hoc.weatherapp.koin.sharePrefUtilModule
import com.hoc.weatherapp.utils.debug
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.KoinContextHandler
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

@Suppress("unused")
class KoinInitializer : Initializer<Koin> {
  @ExperimentalStdlibApi
  override fun create(context: Context): Koin {
    debug("KoinInitializer", "Initializer")
    return (context.applicationContext as Application).startKoinIfNeeded()
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

@OptIn(ExperimentalStdlibApi::class)
fun Application.startKoinIfNeeded(): Koin {
  return KoinContextHandler.getOrNull() ?: startKoin {
    androidLogger(
        if (BuildConfig.DEBUG) Level.DEBUG
        else Level.NONE
    )

    androidContext(this@startKoinIfNeeded)

    modules(
        listOf(
            retrofitModule,
            dataSourceModule,
            sharePrefUtilModule,
            presenterModule
        )
    )
  }.koin
}