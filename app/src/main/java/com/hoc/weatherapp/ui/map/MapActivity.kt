package com.hoc.weatherapp.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.CityRepository
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.databinding.ActivityMapBinding
import com.hoc.weatherapp.ui.BaseAppCompatActivity
import com.hoc081098.viewbindingdelegate.viewBinding
import org.koin.android.ext.android.inject

@ExperimentalStdlibApi
class MapActivity : BaseAppCompatActivity(
  contentLayoutId = R.layout.activity_map,
  noActionBar = false
) {
  private val binding by viewBinding<ActivityMapBinding>()
  private val cityRepository by inject<CityRepository>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      title = "Maps"
    }

    loadMap(cityRepository.selectedCity)

    binding.bottomNav.setOnNavigationItemSelectedListener {
      binding.webView.loadUrl(
        when (it.itemId) {
          R.id.rain_map ->
            "javascript:map.removeLayer(windLayer);map.removeLayer(tempLayer);map.addLayer(rainLayer);"
          R.id.wind_map ->
            "javascript:map.removeLayer(rainLayer);map.removeLayer(tempLayer);map.addLayer(windLayer);"
          R.id.temperature_map ->
            "javascript:map.removeLayer(windLayer);map.removeLayer(rainLayer);map.addLayer(tempLayer);"
          else -> throw IllegalStateException()
        }
      )
      true
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> true.also { finish() }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun loadMap(city: City?) {
    binding.webView.run {
      @SuppressLint("SetJavaScriptEnabled")
      settings.javaScriptEnabled = true
      loadUrl(
        "file:///android_asset/map.html?lat=${city?.lat ?: 0.0}&lon=${
        city?.lng
          ?: 0.0
        }&k=2.0&appid=${getString(R.string.app_id)}"
      )
      setInitialScale(1)
      settings.loadWithOverviewMode = true
      settings.useWideViewPort = true
      webViewClient = object : WebViewClient() {
        @Suppress("OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
          view?.loadUrl(url)
          return true
        }
      }
    }
  }
}
