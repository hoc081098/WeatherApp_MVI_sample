package com.hoc.weatherapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.ui.LocationActivity.Companion.ACTION_UPDATE_CURRENT_WEATHER
import com.hoc.weatherapp.ui.LocationActivity.Companion.EXTRA_CURRENT_WEATHER
import com.hoc.weatherapp.utils.NOTIFICATION_ID
import com.hoc.weatherapp.utils.SharedPrefUtil
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.showOrUpdateNotification
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
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
        private val weatherRepository by inject<WeatherRepository>()
        private val sharedPrefUtil by inject<SharedPrefUtil>()
        private val compositeDisposable = CompositeDisposable()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            findPreference(getString(R.string.key_show_notification)).run {
                onPreferenceChangeListener = this@SettingFragment
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            compositeDisposable.clear()
        }

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
            val key = preference?.key
            when {
                key == getString(R.string.key_show_notification) && newValue is Boolean -> {
                    onChangeEnableNotification(newValue)
                }
            }
            return true
        }

        private fun onChangeEnableNotification(newValue: Boolean) {
            if (newValue) {
                val city = sharedPrefUtil.selectedCity
                if (city !== null) {
                    weatherRepository.getCurrentWeatherByCity(city)
                        .lastElement()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onError = {},
                            onSuccess = {
                                this@SettingFragment.debug("sendBroadcast")
                                if (sharedPrefUtil.showNotification) {
                                    requireContext().showOrUpdateNotification(it)
                                }

                                LocalBroadcastManager.getInstance(requireContext())
                                    .sendBroadcast(
                                        Intent(ACTION_UPDATE_CURRENT_WEATHER).apply {
                                            putExtra(EXTRA_CURRENT_WEATHER, it)
                                        }
                                    )
                            }
                        )
                        .addTo(compositeDisposable)
                } else {
                    requireContext().cancelNotificationById(NOTIFICATION_ID)
                }
            } else {
                requireContext().cancelNotificationById(NOTIFICATION_ID)
            }
        }
    }
}