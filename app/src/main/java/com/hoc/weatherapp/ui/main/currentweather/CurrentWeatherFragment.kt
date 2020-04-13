package com.hoc.weatherapp.ui.main.currentweather

import android.annotation.SuppressLint
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
import com.hoc.weatherapp.ui.LiveWeatherActivity
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.RefreshIntent
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.ViewState
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.snackBar
import com.hoc.weatherapp.utils.startActivity
import com.hoc.weatherapp.utils.ui.getIconDrawableFromCurrentWeather
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_current_weather.*
import org.koin.android.ext.android.get
import java.text.DecimalFormat

private const val TAG = "currentweather"

@ExperimentalStdlibApi
class CurrentWeatherFragment : MviFragment<CurrentWeatherContract.View,
    CurrentWeatherPresenter>(),
    CurrentWeatherContract.View {
  private var errorSnackBar: Snackbar? = null
  private var refreshSnackBar: Snackbar? = null
  private val refreshInitial = PublishSubject.create<RefreshIntent.InitialRefreshIntent>()

  override fun refreshCurrentWeatherIntent(): Observable<RefreshIntent> {
    return swipe_refresh_layout.refreshes()
        .map { RefreshIntent.UserRefreshIntent }
        .cast<RefreshIntent>()
        .mergeWith(refreshInitial.doOnNext { debug("refreshes initial", TAG) })
        .doOnNext { debug("refreshes", TAG) }
  }

  override fun render(state: ViewState) {
    swipe_refresh_layout.isRefreshing = false

    if (state.weather != null) {
      updateUi(state.weather)
    } else {
      noSelectedCity()
    }
    val error = state.error
    if (error is NoSelectedCityException) {
      noSelectedCity()
    }
    if (error != null && state.showError) {
      errorSnackBar?.dismiss()
      errorSnackBar = view?.snackBar(
          error.message ?: "An error occurred!",
          Snackbar.LENGTH_INDEFINITE
      )
    }
    if (!state.showError) {
      errorSnackBar?.dismiss()
    }

    if (state.showRefreshSuccessfully) {
      refreshSnackBar?.dismiss()
      refreshSnackBar = view?.snackBar(
          "Weather has been updated!",
          Snackbar.LENGTH_INDEFINITE
      )
    } else {
      refreshSnackBar?.dismiss()
    }
  }

  private fun noSelectedCity() {
    image_icon.setImageResource(R.drawable.weather_icon_null)
    @SuppressLint("SetTextI18n")
    text_temperature.text = "__"
    text_main_weather.text = getString(R.string.no_main_weather)
    text_last_update.text = getString(
        R.string.last_updated_none,
        "__/__/__ __:__"
    )
    button_live.visibility = View.GONE
    card_view1.visibility = View.GONE
    card_view2.visibility = View.GONE
  }

  private fun updateUi(weather: CurrentWeather) {
    updateWeatherIcon(
        weatherConditionId = weather.weatherConditionId,
        weatherIcon = weather.weatherIcon
    )
    text_temperature.text = weather.temperatureString
    text_main_weather.text = weather.description
    text_last_update.text = getString(R.string.last_updated, weather.dataTimeString, weather.zoneId)

    /**
     *
     */
    button_live.visibility = View.VISIBLE

    /**
     *
     */
    card_view1.visibility = View.VISIBLE
    text_pressure.text = weather.pressureString
    text_humidity.text = getString(R.string.humidity, weather.humidity)
    text_rain.text = getString(
        R.string.rain_mm,
        NUMBER_FORMAT.format(weather.rainVolumeForThe3HoursMm)
    )
    text_visibility.text = getString(
        R.string.visibility_km,
        NUMBER_FORMAT.format(weather.visibilityKm)
    )

    /**
     *
     */
    card_view2.visibility = View.VISIBLE
    windmill1.winSpeed = weather.winSpeed
    windmill2.winSpeed = weather.winSpeed
    text_wind_dir.text = getString(R.string.wind_direction, weather.winDirection)
    text_wind_speed.text = getString(R.string.wind_speed, weather.winSpeedString)
  }

  override fun createPresenter() = get<CurrentWeatherPresenter>()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_current_weather, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    button_live.setOnClickListener { requireContext().startActivity<LiveWeatherActivity>() }
  }

  override fun onResume() {
    super.onResume()
    refreshInitial.onNext(RefreshIntent.InitialRefreshIntent)
  }

  private fun updateWeatherIcon(weatherIcon: String, weatherConditionId: Long) {
    Glide.with(this)
        .load(
            requireContext().getIconDrawableFromCurrentWeather(
                weatherIcon = weatherIcon,
                weatherConditionId = weatherConditionId
            )
        )
        .apply(RequestOptions.fitCenterTransform().centerCrop())
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(image_icon)
  }

  companion object {
    private val NUMBER_FORMAT = DecimalFormat("#.#")
  }
}
