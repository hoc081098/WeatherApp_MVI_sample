package com.hoc.weatherapp.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.local.SelectedCityPreference
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.getOrNull
import kotlinx.android.synthetic.main.activity_map.*
import org.koin.android.ext.android.inject

class MapActivity : AppCompatActivity() {
    private val cityPreference by inject<SelectedCityPreference>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(R.layout.activity_map)

        loadMap(cityPreference.value.getOrNull())

        bottom_nav.setOnNavigationItemSelectedListener {
            web_view.loadUrl(
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

    private fun loadMap(city: City?) {
        web_view.run {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            loadUrl(
                "file:///android_asset/map.html?lat=${city?.lat ?: 0.0}&lon=${city?.lng
                    ?: 0.0}&k=2.0&appid=${getString(R.string.app_id)}"
            )
            setInitialScale(1)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = object: WebViewClient() {
                @Suppress("OverridingDeprecatedMember")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                    view?.loadUrl(url)
                    return true
                }
            }
        }
    }
}