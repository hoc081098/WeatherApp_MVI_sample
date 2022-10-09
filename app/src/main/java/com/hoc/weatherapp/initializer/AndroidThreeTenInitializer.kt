package com.hoc.weatherapp.initializer

import android.content.Context
import androidx.startup.Initializer
import com.hoc.weatherapp.utils.debug
import com.jakewharton.threetenabp.AndroidThreeTen

@Suppress("unused")
class AndroidThreeTenInitializer : Initializer<Unit> {
  override fun create(context: Context) = AndroidThreeTen.init(context)
    .also { debug("AndroidThreeTenInitializer", "Initializer") }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
