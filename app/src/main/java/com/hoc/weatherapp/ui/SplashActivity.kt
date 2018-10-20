package com.hoc.weatherapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hoc.weatherapp.ui.main.MainActivity
import com.hoc.weatherapp.utils.startActivity

class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    startActivity<MainActivity>()
    finish()
  }
}