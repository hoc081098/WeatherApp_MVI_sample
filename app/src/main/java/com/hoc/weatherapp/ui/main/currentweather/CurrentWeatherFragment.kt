package com.hoc.weatherapp.ui.main.currentweather

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.databinding.FragmentCurrentWeatherBinding
import com.hoc.weatherapp.ui.BaseMviFragment
import com.hoc.weatherapp.ui.LiveWeatherActivity
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.RefreshIntent
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.ViewState
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.snackBar
import com.hoc.weatherapp.utils.startActivity
import com.hoc.weatherapp.utils.ui.getIconDrawableFromCurrentWeather
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import io.reactivex.subjects.PublishSubject
import java.text.DecimalFormat
import org.koin.android.ext.android.get

private const val TAG = "currentweather"

@ExperimentalStdlibApi
class CurrentWeatherFragment :
  BaseMviFragment<CurrentWeatherContract.View,
    CurrentWeatherPresenter>(R.layout.fragment_current_weather),
  CurrentWeatherContract.View {
  private val binding by viewBinding<FragmentCurrentWeatherBinding>()

  private var errorSnackBar: Snackbar? = null
  private var refreshSnackBar: Snackbar? = null
  private val refreshInitial = PublishSubject.create<RefreshIntent.InitialRefreshIntent>()

  override fun refreshCurrentWeatherIntent(): Observable<RefreshIntent> {
    return binding.swipeRefreshLayout.refreshes()
      .map { RefreshIntent.UserRefreshIntent }
      .cast<RefreshIntent>()
      .mergeWith(refreshInitial.doOnNext { debug("refreshes initial", TAG) })
      .doOnNext { debug("refreshes", TAG) }
  }

  override fun render(state: ViewState) {
    binding.swipeRefreshLayout.isRefreshing = false

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

  private fun noSelectedCity() = binding.run {
    imageIcon.setImageResource(R.drawable.weather_icon_null)
    @SuppressLint("SetTextI18n")
    textTemperature.text = "__"
    textMainWeather.text = getString(R.string.no_main_weather)
    textLastUpdate.text = getString(
      R.string.last_updated_none,
      "__/__/__ __:__"
    )
    buttonLive.visibility = View.GONE
    cardView1.visibility = View.GONE
    cardView2.visibility = View.GONE
  }

  private fun updateUi(weather: CurrentWeather) = binding.run {
    updateWeatherIcon(
      weatherConditionId = weather.weatherConditionId,
      weatherIcon = weather.weatherIcon
    )
    textTemperature.text = weather.temperatureString
    textMainWeather.text = weather.description
    textLastUpdate.text = getString(R.string.last_updated, weather.dataTimeString, weather.zoneId)

    /**
     *
     */
    buttonLive.visibility = View.VISIBLE

    /**
     *
     */
    cardView1.visibility = View.VISIBLE
    textPressure.text = weather.pressureString
    textHumidity.text = getString(R.string.humidity, weather.humidity)
    textRain.text = getString(
      R.string.rain_mm,
      NUMBER_FORMAT.format(weather.rainVolumeForThe3HoursMm)
    )
    textVisibility.text = getString(
      R.string.visibility_km,
      NUMBER_FORMAT.format(weather.visibilityKm)
    )

    /**
     *
     */
    cardView2.visibility = View.VISIBLE
    windmill1.winSpeed = weather.winSpeed
    windmill2.winSpeed = weather.winSpeed
    textWindDir.text = getString(R.string.wind_direction, weather.winDirection)
    textWindSpeed.text = getString(R.string.wind_speed, weather.winSpeedString)
  }

  override fun createPresenter() = get<CurrentWeatherPresenter>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.buttonLive.setOnClickListener { requireContext().startActivity<LiveWeatherActivity>() }
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
      .into(binding.imageIcon)
  }

  companion object {
    private val NUMBER_FORMAT = DecimalFormat("#.#")
  }
}
