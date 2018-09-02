package com.hoc.weatherapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.AddCityActivity.Companion.ACTION_CHANGED_LOCATION
import com.hoc.weatherapp.AddCityActivity.Companion.SELECTED_CITY
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.utils.getIconDrawableFromIconString
import com.hoc.weatherapp.utils.toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_current_weather.*
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Locale

class CurrentWeatherFragment : Fragment() {
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private val weatherRepository by inject<WeatherRepository>()

    private lateinit var mainActivity: MainActivity

    private val compositeDisposable = CompositeDisposable()
    private val currentWeatherFragmentReceiver = CurrentWeatherFragmentReceiver()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_current_weather, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh_layout.setOnRefreshListener { getCurrentWeather(sharedPrefUtil.selectedCity) }

        getCurrentWeather(sharedPrefUtil.selectedCity)
        LocalBroadcastManager.getInstance(mainActivity)
            .registerReceiver(
                currentWeatherFragmentReceiver,
                IntentFilter().apply {
                    addAction(ACTION_CHANGED_LOCATION)
                }
            )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
        LocalBroadcastManager.getInstance(mainActivity)
            .unregisterReceiver(currentWeatherFragmentReceiver)
    }

    private fun updateUi(weather: CurrentWeather?) {
        when (weather) {
            null -> {
                image_icon.setImageDrawable(null)
                text_temperature.text = ""
                text_main_weather.text = ""
                text_last_update.text = ""
                button_live.visibility = View.INVISIBLE
            }
            else -> {
                updateWeatherIcon(weather.icon)
                text_temperature.text = "${weather.temperature} ℃"
                text_main_weather.text = weather.main
                text_last_update.text = "${sdf.format(weather.dataTime)} update"
                button_live.visibility = View.VISIBLE
            }
        }
    }

    private fun updateWeatherIcon(icon: String) {
        Glide.with(this)
            .load(getIconDrawableFromIconString(icon))
            .apply(RequestOptions.fitCenterTransform().centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(image_icon)
    }

    private fun getCurrentWeather(city: City?) {
        when (city) {
            null -> {
                updateUi(null)
                mainActivity.updateUi(null)
                sharedPrefUtil.selectedCity = null

                toast("Please select a city!")

                mainActivity.enqueueWorkRequest()
            }
            else -> weatherRepository.getCurrentWeatherByCity(city)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    swipe_refresh_layout.post {
                        swipe_refresh_layout.isRefreshing = true
                    }
                }
                .subscribeBy(
                    onError = {
                        toast(it.message ?: "An error occurred")

                        mainActivity.enqueueWorkRequest()
                        swipe_refresh_layout.post {
                            swipe_refresh_layout.isRefreshing = false
                        }
                    },
                    onNext = {
                        updateUi(it)
                        mainActivity.updateUi(it)
                        sharedPrefUtil.selectedCity = it.city

                        toast("Get current weather successfully")

                        mainActivity.enqueueWorkRequest()
                        swipe_refresh_layout.post {
                            swipe_refresh_layout.isRefreshing = false
                        }
                    },
                    onComplete = {

                    }
                )
                .addTo(compositeDisposable)
        }
    }

    private inner class CurrentWeatherFragmentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CHANGED_LOCATION -> getCurrentWeather(intent.getParcelableExtra(SELECTED_CITY))
            }
        }
    }

    companion object {
        @JvmField
        val sdf = SimpleDateFormat("dd/MM/yy hh:mm", Locale.US)
    }
}

