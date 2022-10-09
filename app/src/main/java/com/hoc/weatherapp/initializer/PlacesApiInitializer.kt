package com.hoc.weatherapp.initializer

import android.content.Context
import androidx.startup.Initializer
import com.google.android.libraries.places.api.Places
import com.hoc.weatherapp.BuildConfig
import com.hoc.weatherapp.utils.debug

@Suppress("unused")
class PlacesApiInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    debug("PlacesApiInitializer", "Initializer")

    Places.initialize(context, BuildConfig.PLACE_API_KEY)
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
