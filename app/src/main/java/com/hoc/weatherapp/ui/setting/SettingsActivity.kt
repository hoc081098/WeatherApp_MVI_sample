package com.hoc.weatherapp.ui.setting

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.TemperatureUnit
import org.koin.android.ext.android.inject

class SettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      title = "Settings"
    }

    if (supportFragmentManager.findFragmentById(android.R.id.content) === null) {
      supportFragmentManager.beginTransaction()
        .add(android.R.id.content, SettingFragment())
        .commit()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      android.R.id.home -> true.also { finish() }
      else -> super.onOptionsItemSelected(item)
    }
  }

  class SettingFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    private val sharedPrefUtil by inject<SharedPrefUtil>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences, rootKey)

      findPreference(getString(R.string.key_show_notification)).onPreferenceChangeListener = this
      findPreference(getString(R.string.key_temperature_unit)).onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
      val key = preference?.key
      when {
        key == getString(R.string.key_show_notification) && newValue is Boolean -> {

        }
        key == getString(R.string.key_temperature_unit) && newValue is String -> {
          sharedPrefUtil.temperatureUnit = TemperatureUnit.fromString(newValue)
        }
      }
      return true
    }
  }
}