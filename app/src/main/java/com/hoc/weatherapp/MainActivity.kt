package com.hoc.weatherapp

import android.app.NotificationManager
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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.AddCityActivity.Companion.ACTION_CHANGED_LOCATION
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.utils.NOTIFICATION_ID
import com.hoc.weatherapp.utils.ZoomOutPageTransformer
import com.hoc.weatherapp.utils.blur.GlideBlurTransformation
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getBackgroundDrawableFromIconString
import com.hoc.weatherapp.utils.showOrUpdateNotification
import com.hoc.weatherapp.work.UpdateWeatherWorker
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

        setSupportActionBar(toolbar.apply {
            setNavigationIcon(R.drawable.ic_playlist_add_white_24dp)
        })
        supportActionBar?.setDisplayShowTitleEnabled(false)

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
                DailyWeatherFragment()
            )
            adapter = SectionsPagerAdapter(supportFragmentManager, fragments)
                .also { pagerAdapter = it }
            offscreenPageLimit = 2
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
        val workManager = WorkManager.getInstance()
        val workRequest = PeriodicWorkRequestBuilder<UpdateWeatherWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_UNIQ_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        workManager.getStatusById(workRequest.id).observe(this, Observer {
            if (it != null && it.state.isFinished) {
                debug("UpdateWeatherWorker isFinished", "MAIN_TAG")
            }
        })
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
            android.R.id.home -> {
                startActivity(Intent(this, LocationActivity::class.java))
                true
            }

            R.id.action_settings -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateBackground(icon: String) {
        Glide.with(this)
            .load(getBackgroundDrawableFromIconString(icon))
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions.fitCenterTransform().centerCrop())
            .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
            .into(image_background)
    }

    fun updateUi(weather: CurrentWeather?) {
        when (weather) {
            null -> {
                image_background.setImageResource(R.drawable.default_bg)
                toolbar_title.text = ""
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(NOTIFICATION_ID)
            }
            else -> {
                updateBackground(weather.icon)
                toolbar_title.text = "${weather.city.name} - ${weather.city.country}"
                showOrUpdateNotification(weather)
            }
        }
    }

    private inner class MainActivityBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CHANGED_LOCATION -> enableIndicatorAndViewPager()
            }
        }
    }

    companion object {
        private const val WORK_UNIQ_NAME = "UNIQ_NAME"
    }
}