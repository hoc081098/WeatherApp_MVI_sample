package com.hoc.weatherapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.WindDirection
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.remote.TemperatureUnit.Companion.NUMBER_FORMAT
import com.hoc.weatherapp.ui.LiveWeatherActivity
import com.hoc.weatherapp.ui.main.CurrentWeatherContract.ViewState
import com.hoc.weatherapp.ui.main.CurrentWeatherContract.ViewState.*
import com.hoc.weatherapp.utils.*
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_current_weather.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class CurrentWeatherFragment : MviFragment<CurrentWeatherContract.View, CurrentWeatherPresenter>(),
    CurrentWeatherContract.View {
    private val sharedPrefUtil by inject<SharedPrefUtil>()

    private var errorSnackbar: Snackbar? = null

    private var weatherSnackBar: Snackbar? = null

    private var noSelectedCitySnackbar: Snackbar? = null

    override fun refreshCurrentWeatherIntent(): Observable<Unit> {
        return swipe_refresh_layout.refreshes()
    }

    override fun render(state: ViewState) {
        when (state) {
            is Loading -> renderLoading()
            is Weather -> renderWeather(state)
            is Error -> renderError(state)
            is NoSelectedCity -> renderNoSelectedCity(state)
        }
    }

    private fun renderError(error: Error) {
        setRefreshingSwipeLayout(false)
        if (error.showMessage) {
            errorSnackbar = view?.snackBar(error.throwable.message ?: "An error occurred!")
        } else {
            errorSnackbar?.dismiss()
        }
    }

    private fun renderWeather(weather: Weather) {
        setRefreshingSwipeLayout(false)
        if (weather.showMessage) {
            weatherSnackBar =
                    view?.snackBar("Weather has been updated!", Snackbar.LENGTH_INDEFINITE)
        } else {
            weatherSnackBar?.dismiss()
        }

        val temperature = UnitConvertor.convertTemperature(
            weather.weather.temperature,
            sharedPrefUtil.temperatureUnit
        )
        updateWeatherIcon(weather.weather)
        text_temperature.text =
                getString(R.string.temperature_degree, NUMBER_FORMAT.format(temperature))
        text_main_weather.text = weather.weather.description.capitalize()
        text_last_update.text =
                getString(
                    R.string.last_updated,
                    SIMPLE_DATE_FORMAT.format(weather.weather.dataTime)
                )
        button_live.visibility = View.VISIBLE
        card_view1.visibility = View.VISIBLE
        text_pressure.text = "${weather.weather.pressure}hPa"
        text_humidity.text = getString(R.string.humidity, weather.weather.humidity)
        text_rain.text = "${"%.1f".format(weather.weather.rainVolumeForThe3Hours)}mm"
        text_visibility.text = "${"%.1f".format(weather.weather.visibility / 1_000)}km"
        card_view2.visibility = View.VISIBLE
        windmill1.winSpeed = weather.weather.winSpeed
        windmill2.winSpeed = weather.weather.winSpeed
        text_wind_dir.text = getString(
            R.string.wind_direction,
            WindDirection.fromDegrees(weather.weather.winDegrees)
        )
        text_wind_speed.text = "Speed: ${weather.weather.winSpeed}m/s"
    }

    private fun renderLoading() {
        setRefreshingSwipeLayout(true)
    }

    private fun renderNoSelectedCity(state: NoSelectedCity) {
        setRefreshingSwipeLayout(false)
        if (state.showMessage) {
            noSelectedCitySnackbar = view?.snackBar("Please select a city!")
        } else {
            noSelectedCitySnackbar?.dismiss()
        }

        image_icon.setImageDrawable(null)
        text_temperature.text = ""
        text_main_weather.text = ""
        text_last_update.text = ""
        button_live.visibility = View.INVISIBLE
        card_view1.visibility = View.INVISIBLE
        card_view2.visibility = View.INVISIBLE
    }

    private fun setRefreshingSwipeLayout(isRefreshing: Boolean) {
        swipe_refresh_layout.post {
            swipe_refresh_layout.isRefreshing = isRefreshing
        }
    }

    override fun createPresenter(): CurrentWeatherPresenter {
        return CurrentWeatherPresenter(get())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_current_weather, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_live.setOnClickListener { requireContext().startActivity<LiveWeatherActivity>() }
    }

    private fun updateWeatherIcon(weather: CurrentWeather) {
        Glide.with(this)
            .load(requireContext().getIconDrawableFromCurrentWeather(weather))
            .apply(RequestOptions.fitCenterTransform().centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(image_icon)
    }

    companion object {
        @JvmField
        val SIMPLE_DATE_FORMAT = SimpleDateFormat("dd/MM/yy HH:mm", Locale.US)
    }
}
