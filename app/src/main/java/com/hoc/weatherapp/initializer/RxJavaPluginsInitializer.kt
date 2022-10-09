package com.hoc.weatherapp.initializer

import android.content.Context
import androidx.startup.Initializer
import com.hoc.weatherapp.BuildConfig
import com.hoc.weatherapp.utils.debug
import io.reactivex.plugins.RxJavaPlugins

@Suppress("unused")
class RxJavaPluginsInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    debug("RxJavaPluginsInitializer", "Initializer")

    if (!BuildConfig.DEBUG) {
      RxJavaPlugins.setErrorHandler { debug("RxJava error: $it", "RXJAVA_ERROR", it) }
    }
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
