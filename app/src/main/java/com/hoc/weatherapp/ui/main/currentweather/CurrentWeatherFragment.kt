package com.hoc.weatherapp.ui.main.currentweather

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
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.models.WindDirection
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.remote.TemperatureUnit.Companion.NUMBER_FORMAT
import com.hoc.weatherapp.ui.LiveWeatherActivity
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.ViewState
import com.hoc.weatherapp.utils.*
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_current_weather.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "@#$%"

class CurrentWeatherFragment : MviFragment<CurrentWeatherContract.View, CurrentWeatherPresenter>(),
  CurrentWeatherContract.View {
  private val sharedPrefUtil by inject<SharedPrefUtil>()
  private var errorSnackBar: Snackbar? = null
  private var refreshSnackBar: Snackbar? = null

  override fun refreshCurrentWeatherIntent(): Observable<Unit> {
    return swipe_refresh_layout.refreshes()
      .doOnNext { debug("swipe_refresh_layout.refreshes", TAG) }
  }

  override fun render(state: ViewState) {
    swipe_refresh_layout.isRefreshing = false

    if (state.weather != null) {
      updateUi(state.weather)
    } else {
      noSelectedCity()
    }
    if (state.error != null) {
      if (state.showError) {
        errorSnackBar = view?.snackBar(
          state.error.message ?: "An error occurred!",
          Snackbar.LENGTH_INDEFINITE
        )
      }
      if (state.error is NoSelectedCityException) {
        noSelectedCity()
      }
    }
    if (!state.showError) {
      errorSnackBar?.dismiss()
    }
    if (state.showRefreshSuccessfully) {
      refreshSnackBar = view?.snackBar(
        "Weather has been updated!",
        Snackbar.LENGTH_INDEFINITE
      )
    } else {
      refreshSnackBar?.dismiss()
    }
  }

  private fun noSelectedCity() {
    image_icon.setImageDrawable(null)
    text_temperature.text = ""
    text_main_weather.text = ""
    text_last_update.text = ""
    button_live.visibility = View.INVISIBLE
    card_view1.visibility = View.INVISIBLE
    card_view2.visibility = View.INVISIBLE
  }

  private fun updateUi(currentWeather: CurrentWeather) {
    val temperature = UnitConvertor.convertTemperature(
      currentWeather.temperature,
      sharedPrefUtil.temperatureUnit
    )
    updateWeatherIcon(currentWeather)
    text_temperature.text =
        getString(R.string.temperature_degree, NUMBER_FORMAT.format(temperature))
    text_main_weather.text = currentWeather.description.capitalize()
    text_last_update.text =
        getString(
          R.string.last_updated,
          SIMPLE_DATE_FORMAT.format(currentWeather.dataTime)
        )
    button_live.visibility = View.VISIBLE
    card_view1.visibility = View.VISIBLE
    text_pressure.text = "${currentWeather.pressure}hPa"
    text_humidity.text = getString(R.string.humidity, currentWeather.humidity)
    text_rain.text = "${"%.1f".format(currentWeather.rainVolumeForThe3Hours)}mm"
    text_visibility.text = "${"%.1f".format(currentWeather.visibility / 1_000)}km"
    card_view2.visibility = View.VISIBLE
    windmill1.winSpeed = currentWeather.winSpeed
    windmill2.winSpeed = currentWeather.winSpeed
    text_wind_dir.text = getString(
      R.string.wind_direction,
      WindDirection.fromDegrees(currentWeather.winDegrees)
    )
    text_wind_speed.text = "Speed: ${currentWeather.winSpeed}m/s"
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
      .load(
        requireContext().getIconDrawableFromCurrentWeather(
          weatherIcon = weather.icon,
          weatherConditionId = weather.weatherConditionId
        )
      )
      .apply(RequestOptions.fitCenterTransform().centerCrop())
      .transition(DrawableTransitionOptions.withCrossFade())
      .into(image_icon)
  }

  companion object {
    @JvmField
    val SIMPLE_DATE_FORMAT = SimpleDateFormat("dd/MM/yy HH:mm", Locale.US)
  }
}
