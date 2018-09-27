package com.hoc.weatherapp.ui.main

import android.content.IntentFilter
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.local.LocalDataSource
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.ui.AddCityActivity.Companion.ACTION_CHANGED_LOCATION
import com.hoc.weatherapp.ui.AddCityActivity.Companion.EXTRA_SELECTED_CITY
import com.hoc.weatherapp.ui.LocationActivity
import com.hoc.weatherapp.ui.MapActivity
import com.hoc.weatherapp.ui.SettingsActivity
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.SharedPrefUtil
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.ZoomOutPageTransformer
import com.hoc.weatherapp.utils.blur.GlideBlurTransformation
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getBackgroundDrawableFromWeather
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.getSoundUriFromCurrentWeather
import com.hoc.weatherapp.utils.receivesLocal
import com.hoc.weatherapp.utils.showOrUpdateNotification
import com.hoc.weatherapp.utils.startActivity
import com.hoc.weatherapp.utils.toOptional
import com.hoc.weatherapp.work.UpdateCurrentWeatherWorker
import com.hoc.weatherapp.work.UpdateDailyWeatherWork
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var pagerAdapter: SectionsPagerAdapter? = null
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private val localDataSource by inject<LocalDataSource>()

    private val compositeDisposable = CompositeDisposable()
    private val compositeDisposable1 = CompositeDisposable()

    private var mediaPlayer: MediaPlayer? = null
    private val cityFlowable = BehaviorProcessor.create<Optional<City>>()

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

        @Suppress("UnstableApiUsage")
        receivesLocal(
            IntentFilter().apply {
                addAction(ACTION_CHANGED_LOCATION)
            }
        ).filter { it.action == ACTION_CHANGED_LOCATION }
            .map { it.getParcelableExtra<City?>(EXTRA_SELECTED_CITY).toOptional() }
            .startWith(sharedPrefUtil.selectedCity.toOptional())
            .subscribe(cityFlowable)

        cityFlowable
            .subscribeBy(
                onError = {},
                onNext = {
                    enableIndicatorAndViewPager(it.getOrNull())
                }
            ).addTo(compositeDisposable1)
    }

    override fun onStart() {
        super.onStart()

        cityFlowable
            .ofType<Some<City>>()
            .map { it.value }
            .switchMap {
                localDataSource.getCurrentWeatherById(it.id)
                    .subscribeOn(Schedulers.io())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {},
                onNext = { weather ->
                    debug("onNext $weather", "@@@")

                    runCatching {
                        mediaPlayer?.takeIf { it.isPlaying }?.stop()
                    }
                    mediaPlayer =
                        MediaPlayer.create(this, getSoundUriFromCurrentWeather(weather))
                            .apply {
                                setVolume(0.1f, 0.1f)
                                runCatching { start() }
                                    .onSuccess { debug("MediaPlayer::start", "@@@") }
                            }
                }
            )
            .addTo(compositeDisposable)
    }

    override fun onStop() {
        super.onStop()

        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
        }
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable1.clear()

        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
        }
        // free memory
        mediaPlayer?.release()
        mediaPlayer = null
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
            ).also { pagerAdapter = it }
            offscreenPageLimit = fragments.size
            setPageTransformer(true, ZoomOutPageTransformer())

            dots_indicator.setViewPager(view_pager)
            dots_indicator.setDotsClickable(true)

            enableIndicatorAndViewPager(sharedPrefUtil.selectedCity)
        }
    }

    private fun enableIndicatorAndViewPager(selectedCity: City?) {
        if (selectedCity !== null) {
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
            R.id.action_map -> true.also { startActivity<MapActivity>() }
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

    fun updateUi(weather: CurrentWeather?) {
        when (weather) {
            null -> {
                Glide.with(this)
                    .load(R.drawable.default_bg)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.fitCenterTransform().centerCrop())
                    .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
                    .into(image_background)
                toolbar_title.text = getString(R.string.no_selected_city)
                cancelNotificationById(WEATHER_NOTIFICATION_ID)
            }
            else -> {
                updateBackground(weather)
                toolbar_title.text = getString(
                    R.string.city_name_and_country,
                    weather.city.name,
                    weather.city.country
                )
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
}

