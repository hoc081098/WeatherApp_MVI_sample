package com.hoc.weatherapp.ui.main

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
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.data.models.WindDirection
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.remote.TemperatureUnit
import com.hoc.weatherapp.data.remote.TemperatureUnit.Companion.NUMBER_FORMAT
import com.hoc.weatherapp.ui.AddCityActivity.Companion.ACTION_CHANGED_LOCATION
import com.hoc.weatherapp.ui.AddCityActivity.Companion.EXTRA_SELECTED_CITY
import com.hoc.weatherapp.ui.LiveWeatherActivity
import com.hoc.weatherapp.ui.LocationActivity
import com.hoc.weatherapp.ui.LocationActivity.Companion.ACTION_UPDATE_CURRENT_WEATHER
import com.hoc.weatherapp.ui.SettingsActivity.SettingFragment.Companion.ACTION_CHANGED_TEMPERATURE_UNIT
import com.hoc.weatherapp.ui.SettingsActivity.SettingFragment.Companion.EXTRA_TEMPERATURE_UNIT
import com.hoc.weatherapp.utils.SharedPrefUtil
import com.hoc.weatherapp.utils.UnitConvertor
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getIconDrawableFromIconString
import com.hoc.weatherapp.utils.showOrUpdateNotification
import com.hoc.weatherapp.utils.snackBar
import com.hoc.weatherapp.utils.startActivity
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
    private val localBroadcastManager by lazy(LazyThreadSafetyMode.NONE) {
        LocalBroadcastManager.getInstance(mainActivity)
    }
    private var lastestCurrentWeather: CurrentWeather? = null

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

        button_live.setOnClickListener { requireContext().startActivity<LiveWeatherActivity>() }

        getCurrentWeather(sharedPrefUtil.selectedCity)

        localBroadcastManager
            .registerReceiver(
                currentWeatherFragmentReceiver,
                IntentFilter().apply {
                    addAction(ACTION_CHANGED_LOCATION)
                    addAction(ACTION_UPDATE_CURRENT_WEATHER)
                    addAction(ACTION_CHANGED_TEMPERATURE_UNIT)
                }
            )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
        localBroadcastManager.unregisterReceiver(currentWeatherFragmentReceiver)
    }

    private fun updateUi(weather: CurrentWeather?) {
        lastestCurrentWeather = weather

        when (weather) {
            null -> {
                image_icon.setImageDrawable(null)
                text_temperature.text = ""
                text_main_weather.text = ""
                text_last_update.text = ""
                button_live.visibility = View.INVISIBLE
                card_view1.visibility = View.INVISIBLE
                card_view2.visibility = View.INVISIBLE
            }
            else -> {
                val temperature = UnitConvertor.convertTemperature(
                    weather.temperature,
                    sharedPrefUtil.temperatureUnit
                )
                updateWeatherIcon(weather.icon)
                text_temperature.text =
                    getString(R.string.temperature_degree, NUMBER_FORMAT.format(temperature))
                text_main_weather.text = weather.description.capitalize()
                text_last_update.text =
                    getString(R.string.last_updated, SIMPLE_DATE_FORMAT.format(weather.dataTime))
                button_live.visibility = View.VISIBLE

                card_view1.visibility = View.VISIBLE
                text_pressure.text = "${weather.pressure}hPa"
                text_humidity.text = getString(R.string.humidity, weather.humidity)
                text_rain.text = "${"%.1f".format(weather.rainVolumeForTheLast3Hours)}mm"
                text_visibility.text = "${"%.1f".format(weather.visibility / 1_000)}km"

                card_view2.visibility = View.VISIBLE
                windmill1.winSpeed = weather.winSpeed
                windmill2.winSpeed = weather.winSpeed
                text_wind_dir.text = getString(
                    R.string.wind_direction,
                    WindDirection.fromDegrees(weather.winDegrees)
                )
                text_wind_speed.text = "Speed: ${weather.winSpeed}m/s"
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
        swipe_refresh_layout.post { swipe_refresh_layout.isRefreshing = city !== null }

        when (city) {
            null -> {
                updateUi(null)
                mainActivity.updateUi(null)
                sharedPrefUtil.selectedCity = null

                view?.snackBar("Please select a city!")

                mainActivity.cancelWorkRequest()
            }
            else -> weatherRepository.getCurrentWeatherByCity(city)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = {
                        view?.snackBar(it.message ?: "An error occurred")

                        mainActivity.enqueueWorkRequest()
                        swipe_refresh_layout.post {
                            swipe_refresh_layout.isRefreshing = false
                        }
                    },
                    onNext = {
                        updateUi(it)
                        mainActivity.updateUi(it)
                        sharedPrefUtil.selectedCity = it.city

                        view?.snackBar("Get current weather successfully")

                        mainActivity.enqueueWorkRequest()
                        swipe_refresh_layout.post {
                            swipe_refresh_layout.isRefreshing = false
                        }
                    }
                )
                .addTo(compositeDisposable)
        }
    }

    private inner class CurrentWeatherFragmentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CHANGED_LOCATION -> {
                    getCurrentWeather(intent.getParcelableExtra(EXTRA_SELECTED_CITY))
                }
                ACTION_UPDATE_CURRENT_WEATHER -> {
                    intent.getParcelableExtra<CurrentWeather>(LocationActivity.EXTRA_CURRENT_WEATHER)
                        ?.takeIf { it.city.id == sharedPrefUtil.selectedCity?.id }
                        ?.let {
                            this@CurrentWeatherFragment.debug("ACTION_UPDATE_CURRENT_WEATHER")
                            updateUi(it)
                            mainActivity.updateUi(it)
                            sharedPrefUtil.selectedCity = it.city
                            mainActivity.enqueueWorkRequest()
                        }
                }
                ACTION_CHANGED_TEMPERATURE_UNIT -> {
                    intent.getStringExtra(EXTRA_TEMPERATURE_UNIT)
                        .let { TemperatureUnit.fromString(it) }
                        .let(::onChangedTemperatureUnit)
                }
            }
        }
    }

    private fun onChangedTemperatureUnit(unit: TemperatureUnit) = lastestCurrentWeather?.let {
        val temperature = UnitConvertor.convertTemperature(
            it.temperature,
            unit
        )
        text_temperature.text =
            getString(R.string.temperature_degree, NUMBER_FORMAT.format(temperature))
        requireContext().showOrUpdateNotification(it, unit)
        debug("CurrentWeatherFragment::onChangedTemperatureUnit unit=$unit", "@@@")
    }

    companion object {
        @JvmField
        val SIMPLE_DATE_FORMAT = SimpleDateFormat("dd/MM/yy HH:mm", Locale.US)
    }
}

