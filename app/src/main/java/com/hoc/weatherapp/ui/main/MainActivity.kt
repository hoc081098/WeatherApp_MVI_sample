package com.hoc.weatherapp.ui.main

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.ui.LocationsActivity
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.utils.blur.GlideBlurTransformation
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class MainActivity : MviActivity<MainContract.View, MainPresenter>(), MainContract.View {
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private var pagerAdapter: SectionsPagerAdapter? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_playlist_add_white_24dp)
        }

        setupViewPager()
    }

    override fun onStop() {
        super.onStop()

        stopSound()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopSound()
        // free memory
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun setupViewPager() {
        view_pager.run {
            val fragments = listOf(
                CurrentWeatherFragment()
//                DailyWeatherFragment()
//                ChartFragment()
            )
            adapter = SectionsPagerAdapter(
                supportFragmentManager,
                fragments
            ).also { pagerAdapter = it }
            offscreenPageLimit = fragments.size
            setPageTransformer(true, ZoomOutPageTransformer())

            dots_indicator.setViewPager(view_pager)
            dots_indicator.setDotsClickable(true)
        }
    }

    private fun enableIndicatorAndViewPager(isEnable: Boolean) {
        if (isEnable) {
            dots_indicator.visibility = View.VISIBLE
            view_pager.pagingEnable = true
        } else {
            dots_indicator.visibility = View.INVISIBLE
            view_pager.setCurrentItem(0, true)
            view_pager.pagingEnable = false
        }
    }

    fun enqueueWorkRequest() {
//        val updateCurrentWeather =
//            PeriodicWorkRequestBuilder<UpdateCurrentWeatherWorker>(15, TimeUnit.MINUTES)
//                .build()
//
//        val updateDailyWeathers =
//            PeriodicWorkRequestBuilder<UpdateDailyWeatherWork>(15, TimeUnit.MINUTES)
//                .build()
//
//        WorkManager.getInstance().run {
//            enqueueUniquePeriodicWork(
//                UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME,
//                ExistingPeriodicWorkPolicy.REPLACE,
//                updateCurrentWeather
//            )
//            getStatusesForUniqueWork(UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME)
//                .observe(this@MainActivity, Observer {
//                    if (it != null) {
//                        this@MainActivity.debug("${UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME}: $it")
//                    }
//                })
//
//            enqueueUniquePeriodicWork(
//                UpdateDailyWeatherWork.UNIQUE_WORK_NAME,
//                ExistingPeriodicWorkPolicy.REPLACE,
//                updateDailyWeathers
//            )
//            getStatusesForUniqueWork(UpdateDailyWeatherWork.UNIQUE_WORK_NAME)
//                .observe(this@MainActivity, Observer {
//                    if (it != null) {
//                        this@MainActivity.debug("${UpdateDailyWeatherWork.UNIQUE_WORK_NAME}: $it")
//                    }
//                })
//        }
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
            android.R.id.home -> true.also {
                startActivity<LocationsActivity>()
            }
            R.id.action_settings -> true.also {

            }
//            R.id.action_map -> true.also { startActivity<MapActivity>() }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateBackground(weather: CurrentWeather) {
        Glide.with(this)
            .asBitmap()
            .load(getBackgroundDrawableFromWeather(weather))
            .apply(
                RequestOptions
                    .bitmapTransform(GlideBlurTransformation(this, 25f))
                    .fitCenter()
                    .centerCrop()
            )
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(object : CustomViewTarget<ImageView, Bitmap>(image_background) {
                override fun onLoadFailed(errorDrawable: Drawable?) = Unit

                override fun onResourceCleared(placeholder: Drawable?) = Unit

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    view.setImageBitmap(resource)
                    Palette.from(resource)
                        .generate {
                            it ?: return@generate
                            window.statusBarColor = it.getDarkVibrantColor(
                                ContextCompat.getColor(
                                    this@MainActivity,
                                    R.color.colorPrimaryDark
                                )
                            ).also { debug("Color $it", "@@@") }
                        }
                }
            })
    }

    fun cancelWorkRequest() {
//        WorkManager.getInstance().run {
//            cancelUniqueWork(UpdateDailyWeatherWork.UNIQUE_WORK_NAME)
//            cancelUniqueWork(UpdateCurrentWeatherWorker.UNIQUE_WORK_NAME)
//        }
    }

    private fun stopSound() {
        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
        }
    }

    private fun playSound(weather: CurrentWeather) {
        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
        }
        mediaPlayer =
                MediaPlayer.create(this, getSoundUriFromCurrentWeather(weather))
                    .apply {
                        setVolume(0.2f, 0.2f)
                        runCatching { start() }.onSuccess { debug("MediaPlayer::start", "@@@") }
                    }
    }

    override fun render(state: MainContract.ViewState) {
        when (state) {
            MainContract.ViewState.NoSelectedCity -> renderNoSelectedCity()
            is MainContract.ViewState.CityAndWeather -> renderCityAndWeather(state)
        }
    }

    private fun renderCityAndWeather(state: MainContract.ViewState.CityAndWeather) {
        updateBackground(state.weather)
        toolbar_title.text = getString(
            R.string.city_name_and_country,
            state.city.name,
            state.city.country
        )
        if (sharedPrefUtil.showNotification) {
            showOrUpdateNotification(
                state.weather,
                state.city.name,
                state.city.country,
                sharedPrefUtil.temperatureUnit
            )
        }
        enqueueWorkRequest()
        playSound(state.weather)
        enableIndicatorAndViewPager(true)
    }

    private fun renderNoSelectedCity() {
        Glide.with(this)
            .load(R.drawable.default_bg)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions.fitCenterTransform().centerCrop())
            .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
            .into(image_background)
        toolbar_title.text = getString(R.string.no_selected_city)
        cancelNotificationById(WEATHER_NOTIFICATION_ID)
        cancelWorkRequest()
        stopSound()
        enableIndicatorAndViewPager(false)
    }

    override fun createPresenter(): MainPresenter {
        return MainPresenter(get())
    }
}
