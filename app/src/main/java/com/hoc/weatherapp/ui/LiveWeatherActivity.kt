package com.hoc.weatherapp.ui

import android.os.Bundle
import android.view.MenuItem
import com.hoc.weatherapp.R
import kotlinx.android.synthetic.main.activity_live_weather.*

@ExperimentalStdlibApi
class LiveWeatherActivity : BaseAppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_live_weather)

    setSupportActionBar(toolbar)
    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(R.drawable.ic_navigate_before_white_24dp)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        finish()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}