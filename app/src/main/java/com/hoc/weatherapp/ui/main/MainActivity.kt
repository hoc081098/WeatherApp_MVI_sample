package com.hoc.weatherapp.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.ui.AddCityActivity.Companion.ACTION_CHANGED_LOCATION
import com.hoc.weatherapp.ui.LocationActivity
import com.hoc.weatherapp.ui.SettingsActivity
import com.hoc.weatherapp.utils.NOTIFICATION_ID
import com.hoc.weatherapp.utils.SharedPrefUtil
import com.hoc.weatherapp.utils.ZoomOutPageTransformer
import com.hoc.weatherapp.utils.blur.GlideBlurTransformation
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getBackgroundDrawableFromWeather
import com.hoc.weatherapp.utils.showOrUpdateNotification
import com.hoc.weatherapp.utils.startActivity
import com.hoc.weatherapp.work.UpdateCurrentWeatherWorker
import com.hoc.weatherapp.work.UpdateDailyWeatherWork
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var pagerAdapter: SectionsPagerAdapter? = null
    private val sharedPrefUtil by inject<SharedPrefUtil>()

    private val mainActivityBroadcastReceiver = MainActivityBroadcastReceiver()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_playlist_add_white_24dp)
        }

        setupViewPager()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                mainActivityBroadcastReceiver,
                IntentFilter().apply {
                    addAction(ACTION_CHANGED_LOCATION)
                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(mainActivityBroadcastReceiver)
    }

    private fun setupViewPager() {
        view_pager.run {
            val fragments = listOf(
                CurrentWeatherFragment(),
                DailyWeatherFragment(),
                ChartFragment()
            )
            adapter = SectionsPagerAdapter(
                supportFragmentManager,
                fragments
            )
                .also { pagerAdapter = it }
            offscreenPageLimit = 3
            setPageTransformer(true, ZoomOutPageTransformer())

            dots_indicator.setViewPager(view_pager)
            dots_indicator.setDotsClickable(true)

            enableIndicatorAndViewPager()
        }
    }

    fun enableIndicatorAndViewPager() {
        if (sharedPrefUtil.selectedCity !== null) {
            dots_indicator.visibility = View.VISIBLE
            view_pager.pagingEnable = true
        } else {
            dots_indicator.visibility = View.INVISIBLE
            view_pager.setCurrentItem(0, true)
            view_pager.pagingEnable = false
        }
    }

    fun enqueueWorkRequest() {
        val updateCurrentWeather =
            PeriodicWorkRequestBuilder<UpdateCurrentWeatherWorker>(15, TimeUnit.MINUTES)
                .build()

        val updateDailyWeathers =
            PeriodicWorkRequestBuilder<UpdateDailyWeatherWork>(15, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance().run {
            enqueueUniquePeriodicWork(
                UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                updateCurrentWeather
            )
            getStatusesForUniqueWork(UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME)
                .observe(this@MainActivity, Observer {
                    if (it != null) {
                        this@MainActivity.debug("${UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME}: $it")
                    }
                })

            enqueueUniquePeriodicWork(
                UpdateDailyWeatherWork.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                updateDailyWeathers
            )
            getStatusesForUniqueWork(UpdateDailyWeatherWork.UNIQUE_WORK_NAME)
                .observe(this@MainActivity, Observer {
                    if (it != null) {
                        this@MainActivity.debug("${UpdateDailyWeatherWork.UNIQUE_WORK_NAME}: $it")
                    }
                })
        }
    }

    private class SectionsPagerAdapter(fm: FragmentManager, private val fragments: List<Fragment>) :
        FragmentPagerAdapter(fm) {
        override fun getItem(position: Int) = fragments[position]
        override fun getCount() = fragments.size
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> true.also { startActivity<LocationActivity>() }
            R.id.action_settings -> true.also { startActivity<SettingsActivity>() }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateBackground(weather: CurrentWeather) {
        Glide.with(this)
            .load(getBackgroundDrawableFromWeather(weather, this))
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions.fitCenterTransform().centerCrop())
            .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
            .apply(RequestOptions.errorOf(R.drawable.default_bg))
            .into(image_background)
    }

    fun updateUi(weather: CurrentWeather?) {
        when (weather) {
            null -> {
                Glide.with(this)
                    .load(R.drawable.default_bg)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.fitCenterTransform().centerCrop())
                    .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
                    .into(image_background)
                toolbar_title.text = "No selected city"
                cancelNotificationById(NOTIFICATION_ID)
            }
            else -> {
                updateBackground(weather)
                toolbar_title.text = "${weather.city.name} - ${weather.city.country}"
                if (sharedPrefUtil.showNotification) {
                    showOrUpdateNotification(weather, sharedPrefUtil.temperatureUnit)
                }
            }
        }
    }

    fun cancelWorkRequest() {
        WorkManager.getInstance().run {
            cancelUniqueWork(UpdateDailyWeatherWork.UNIQUE_WORK_NAME)
            cancelUniqueWork(UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME)
        }
    }

    private inner class MainActivityBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CHANGED_LOCATION -> enableIndicatorAndViewPager()
            }
        }
    }
}