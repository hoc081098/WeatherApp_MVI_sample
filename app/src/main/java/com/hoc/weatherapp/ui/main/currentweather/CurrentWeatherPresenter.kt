package com.hoc.weatherapp.ui.main.currentweather

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CityRepository
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.WindDirection
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.PartialStateChange
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.RefreshIntent
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.View
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.ViewState
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.WEATHER_NOTIFICATION_ID
import com.hoc.weatherapp.utils.cancelNotificationById
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.exhaustMap
import com.hoc.weatherapp.utils.notOfType
import com.hoc.weatherapp.utils.showNotificationIfEnabled
import com.hoc.weatherapp.utils.toZonedDateTime
import com.hoc.weatherapp.worker.WorkerUtil
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.ofType
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.threeten.bp.format.DateTimeFormatter

@ExperimentalStdlibApi
class CurrentWeatherPresenter(
  private val currentWeatherRepository: CurrentWeatherRepository,
  private val cityRepository: CityRepository,
  private val androidApplication: Application,
  private val settingPreferences: SettingPreferences
) : MviBasePresenter<View, ViewState>() {

  private val cityAndWeatherPartialChange = Observables.combineLatest(
    source1 = settingPreferences.speedUnitPreference.observable,
    source2 = settingPreferences.pressureUnitPreference.observable,
    source3 = settingPreferences.temperatureUnitPreference.observable,
    source4 = currentWeatherRepository.getSelectedCityAndCurrentWeatherOfSelectedCity(),
    combineFunction = { speedUnit, pressureUnit, temperatureUnit, optional ->
      Tuple4(
        speedUnit,
        pressureUnit,
        temperatureUnit,
        optional
      )
    }
  ).switchMap { (speedUnit, pressureUnit, temperatureUnit, optional) ->
    when (optional) {
      None -> showError(NoSelectedCityException)
      is Some -> Observable.just(
        toCurrentWeather(
          optional.value,
          speedUnit,
          pressureUnit,
          temperatureUnit
        )
      ).map<PartialStateChange> { PartialStateChange.Weather(it) }
    }.onErrorResumeNext(::showError)
  }

  private val refreshWeatherProcessor =
    ObservableTransformer<RefreshIntent, PartialStateChange> { intentObservable ->
      intentObservable
        .publish { shared ->
          Observable.mergeArray(
            shared.ofType<RefreshIntent.InitialRefreshIntent>()
              .take(1)
              .delay { cityRepository.getSelectedCity().filter { it is Some } },
            shared.notOfType<RefreshIntent.InitialRefreshIntent>()
          )
        }
        .exhaustMap {
          currentWeatherRepository
            .refreshCurrentWeatherOfSelectedCity()
            .doOnSuccess {
              if (settingPreferences.autoUpdatePreference.value) {
                WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
              }
              androidApplication.showNotificationIfEnabled(it, settingPreferences)
            }
            .doOnError {
              if (it is NoSelectedCityException) {
                androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
                WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
                WorkerUtil.cancelUpdateDailyWeatherWorkRequest()
              }
            }
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .switchMap {
              Observable
                .timer(2_000, TimeUnit.MILLISECONDS)
                .map<PartialStateChange> { PartialStateChange.RefreshWeatherSuccess(showMessage = false) }
                .startWith(PartialStateChange.RefreshWeatherSuccess(showMessage = true))
            }
            .onErrorResumeNext(::showError)
        }
    }

  override fun bindIntents() {
    subscribeViewState(
      Observable.mergeArray(
        intent(View::refreshCurrentWeatherIntent).compose(refreshWeatherProcessor),
        cityAndWeatherPartialChange
      ).scan(ViewState(), reducer)
        .distinctUntilChanged()
        .doOnNext { debug("ViewState=$it", TAG) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private companion object {
    private const val TAG = "__current_weather__"

    private val LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")

    private data class Tuple4(
      val speedUnit: SpeedUnit,
      val pressureUnit: PressureUnit,
      val temperatureUnit: TemperatureUnit,
      val optional: Optional<CityAndCurrentWeather>
    )

    @JvmStatic
    private val reducer =
      BiFunction<ViewState, PartialStateChange, ViewState> { viewState, partialStateChange ->
        when (partialStateChange) {
          is PartialStateChange.Error -> viewState.copy(
            showError = partialStateChange.showMessage,
            error = partialStateChange.throwable,
            weather = if (partialStateChange.throwable is NoSelectedCityException) {
              null
            } else {
              viewState.weather
            }
          )
          is PartialStateChange.Weather -> viewState.copy(
            weather = partialStateChange.weather,
            error = null
          )
          is PartialStateChange.RefreshWeatherSuccess -> viewState.copy(
            showRefreshSuccessfully = partialStateChange.showMessage,
            error = null
          )
        }
      }

    @JvmStatic
    private fun toCurrentWeather(
      cityAndCurrentWeather: CityAndCurrentWeather,
      speedUnit: SpeedUnit,
      pressureUnit: PressureUnit,
      temperatureUnit: TemperatureUnit
    ): CurrentWeather {
      val weather = cityAndCurrentWeather.currentWeather
      val dataTimeString = weather
        .dataTime
        .toZonedDateTime(cityAndCurrentWeather.city.zoneId)
        .format(LAST_UPDATED_FORMATTER)
      return CurrentWeather(
        temperatureString = temperatureUnit.format(weather.temperature),
        pressureString = pressureUnit.format(weather.pressure),
        rainVolumeForThe3HoursMm = weather.rainVolumeForThe3Hours,
        visibilityKm = weather.visibility / 1_000,
        humidity = weather.humidity,
        description = weather.description.replaceFirstChar {
          if (it.isLowerCase()) it.titlecase(
            Locale.ROOT
          ) else it.toString()
        },
        dataTimeString = dataTimeString,
        weatherConditionId = weather.weatherConditionId,
        weatherIcon = weather.icon,
        winSpeed = weather.winSpeed,
        winSpeedString = speedUnit.format(weather.winSpeed),
        winDirection = WindDirection.fromDegrees(weather.winDegrees).toString(),
        zoneId = cityAndCurrentWeather.city.zoneId
      )
    }

    @JvmStatic
    private fun showError(throwable: Throwable): Observable<PartialStateChange> {
      return Observable.timer(2_000, TimeUnit.MILLISECONDS)
        .map<PartialStateChange> {
          PartialStateChange.Error(throwable = throwable, showMessage = false)
        }
        .startWith(
          PartialStateChange.Error(throwable = throwable, showMessage = true)
        )
    }
  }
}
