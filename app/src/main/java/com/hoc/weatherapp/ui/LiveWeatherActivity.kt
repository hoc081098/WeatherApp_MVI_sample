package com.hoc.weatherapp.ui

import android.os.Bundle
import android.view.MenuItem
import com.hoc.weatherapp.R
import com.hoc.weatherapp.databinding.ActivityLiveWeatherBinding
import com.hoc081098.viewbindingdelegate.viewBinding

@ExperimentalStdlibApi
class LiveWeatherActivity : BaseAppCompatActivity(R.layout.activity_live_weather) {
  private val binding by viewBinding<ActivityLiveWeatherBinding>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setSupportActionBar(binding.toolbar)
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
