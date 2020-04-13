package com.hoc.weatherapp.ui

import android.content.Intent
import android.os.Bundle
import com.hoc.weatherapp.ui.main.MainActivity

@ExperimentalStdlibApi
class SplashActivity : BaseAppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    startActivity(Intent(this, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    })
    finish()
  }
}