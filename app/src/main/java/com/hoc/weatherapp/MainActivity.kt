package com.hoc.weatherapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.AddCityActivity.Companion.ACTION_CHANGED_LOCATION
import com.hoc.weatherapp.AddCityActivity.Companion.SELECTED_CITY
import com.hoc.weatherapp.App.Companion.CHANNEL_ID
import com.hoc.weatherapp.data.City
import com.hoc.weatherapp.data.Weather
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.utils.ZoomOutPageTransformer
import com.hoc.weatherapp.utils.blur.GlideBlurTransformation
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.toast
import com.hoc.weatherapp.work.UpdateWeatherWorker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var pagerAdapter: SectionsPagerAdapter? = null
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private val weatherRepository by inject<WeatherRepository>()

    private val mainActivityBroadcastReceiver = MainActivityBroadcastReceiver()
    private lateinit var currentWeatherFragment: CurrentWeatherFragment

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

        getCurrentWeather(sharedPrefUtil.selectedCity)
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
                    CurrentWeatherFragment().also { currentWeatherFragment = it },
                    PlaceHolderFragment()
            )
            adapter = SectionsPagerAdapter(supportFragmentManager, fragments)
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
            view_pager.pagingEnable = false
        }
    }

    private fun showNotification(weather: Weather) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("${weather.city.name} - ${weather.city.country}")
                .setContentText("${weather.main}...${weather.temperature} \u2103")
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(weather.dataTime.time)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        val resultPendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(resultPendingIntent)
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(UpdateWeatherWorker.NOTIFICATION_ID, builder.build())
    }

    private fun enqueueWorkRequest() {
        val workManager = WorkManager.getInstance()
        val workRequest = PeriodicWorkRequestBuilder<UpdateWeatherWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                        Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                )
                .build()

        workManager.enqueueUniquePeriodicWork(
                "UNIQ_NAME",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        )
        workManager.getStatusById(workRequest.id).observe(this, Observer {
            if (it != null && it.state.isFinished) {
                Log.d("MAIN_TAG", "isFinished")
            }
        })
    }

    class SectionsPagerAdapter(fm: FragmentManager, private val fragments: List<Fragment>) :
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
        debug(icon)
        val random = Random()

        @DrawableRes val background = when (icon) {
            "01d" -> R.drawable.sohot
            "01n" -> if (random.nextBoolean()) R.drawable.beautifulnight else R.drawable.moon
            "02d" -> R.drawable.sun
            "02n" -> if (random.nextBoolean()) R.drawable.beautifulnight else R.drawable.moon
            "03d" -> R.drawable.default_bg
            "03n" -> R.drawable.default_bg
            "04d" -> R.drawable.default_bg
            "04n" -> R.drawable.default_bg
            "09d" -> R.drawable.rainy
            "09n" -> R.drawable.rainy
            "10d" -> R.drawable.rainy
            "10n" -> R.drawable.rainy
            "11d" -> R.drawable.rainy
            "11n" -> R.drawable.rainy
            "13d" -> R.drawable.default_bg
            "13n" -> R.drawable.default_bg
            "50d" -> R.drawable.default_bg
            "50n" -> R.drawable.default_bg
            else -> return
        }


        Glide.with(this)
                .load(background)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.fitCenterTransform().centerCrop())
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 20f)))
                .into(image_background)
    }

    inner class MainActivityBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CHANGED_LOCATION -> {
                    getCurrentWeather(intent.getParcelableExtra(SELECTED_CITY))
                    enableIndicatorAndViewPager()
                }
            }
        }
    }

    fun getCurrentWeather(city: City?) {
        if (city != null) {
            toast("Get current weather...")
            weatherRepository.getCurrentWeatherByCity(city)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onError = { toast(it.message ?: "An error occurred") },
                            onNext = {
                                currentWeatherFragment.updateUi(it)

                                updateBackground(it.icon)
                                toolbar_title.text = "${it.city.name} - ${it.city.country}"

                                sharedPrefUtil.selectedCity = it.city
                                showNotification(it)

                                toast("Get current weather successfully")
                            },
                            onComplete = ::enqueueWorkRequest
                    )
                    .addTo(compositeDisposable)
        } else {
            enqueueWorkRequest()
        }
    }
}

class PlaceHolderFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_current_weather, container, false)
}