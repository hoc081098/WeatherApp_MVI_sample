package com.hoc.weatherapp.ui.main.currentweather

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.WindDirection
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.*
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.worker.WorkerUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val TAG = "currrentweather"

class CurrentWeatherPresenter(
  private val currentWeatherRepository: CurrentWeatherRepository,
  private val androidApplication: Application,
  private val settingPreferences: SettingPreferences
) :
  MviBasePresenter<View, ViewState>() {
  override fun bindIntents() {
    subscribeViewState(
      Observable.mergeArray(refreshWeatherPartialChange(), cityAndWeatherPartialChange())
        .scan(ViewState(), ::reduce)
        .distinctUntilChanged()
        .doOnNext { debug("CurrentWeatherPresenter ViewState = $it", TAG) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private data class Tuple4(
    val speedUnit: SpeedUnit,
    val pressureUnit: PressureUnit,
    val temperatureUnit: TemperatureUnit,
    val optional: Optional<CityAndCurrentWeather>
  )

  private fun cityAndWeatherPartialChange(): Observable<PartialStateChange> {
    return Observables.combineLatest(
      settingPreferences.speedUnitPreference.observable.doOnNext { debug("speed=$it", TAG) },
      settingPreferences.pressureUnitPreference.observable.doOnNext { debug("pressure=$it", TAG) },
      settingPreferences.temperatureUnitPreference.observable.doOnNext { debug("temp=$it", TAG) },
      currentWeatherRepository.getSelectedCityAndCurrentWeatherOfSelectedCity().doOnNext {
        debug("current weather and city=$it", TAG)
      }
    ) { speedUnit, pressureUnit, temperatureUnit, optional ->
      Tuple4(
        speedUnit,
        pressureUnit,
        temperatureUnit,
        optional
      )
    }
      .doOnNext { debug("tuple4 = $it", TAG) }
      .switchMap { (speedUnit, pressureUnit, temperatureUnit, optional) ->
        when (optional) {
          None -> showError(NoSelectedCityException)
          is Some -> Observable.just(
            toCurrentWeather(
              optional.value,
              speedUnit,
              pressureUnit,
              temperatureUnit
            )
          ).map { PartialStateChange.Weather(it) }.cast()
        }.onErrorResumeNext { throwable: Throwable -> showError(throwable) }
      }.doOnNext { debug("current weather = $it", TAG) }
  }

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
      description = weather.description.capitalize(),
      dataTimeString = dataTimeString,
      weatherConditionId = weather.weatherConditionId,
      weatherIcon = weather.icon,
      winSpeed = weather.winSpeed,
      winSpeedString = speedUnit.format(weather.winSpeed),
      winDirection = WindDirection.fromDegrees(weather.winDegrees).toString(),
      zoneId = cityAndCurrentWeather.city.zoneId
    )
  }

  private fun refreshWeatherPartialChange(): Observable<PartialStateChange> {
    return intent(View::refreshCurrentWeatherIntent)
      .publish { shared ->
        Observable.mergeArray(
          shared.ofType<RefreshIntent.InitialRefreshIntent>().take(1),
          shared.notOfType<RefreshIntent.InitialRefreshIntent>()
        )
      }
      .doOnNext { debug("refresh intent $it") }
      .exhaustMap {
        currentWeatherRepository
          .refreshCurrentWeatherOfSelectedCity()
          .doOnSuccess {
            if (settingPreferences.autoUpdatePreference.value) {
              WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
            }

            if (settingPreferences.showNotificationPreference.value) {
              androidApplication.showOrUpdateNotification(
                weather = it.currentWeather,
                city = it.city,
                unit = settingPreferences.temperatureUnitPreference.value,
                popUpAndSound = settingPreferences.soundNotificationPreference.value
              )
            }
          }
          .doOnError {
            if (it is NoSelectedCityException) {
              androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
              WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
              WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
            }
          }
          .map {
            toCurrentWeather(
              it,
              settingPreferences.speedUnitPreference.value,
              settingPreferences.pressureUnitPreference.value,
              settingPreferences.temperatureUnitPreference.value
            )
          }
          .toObservable()
          .observeOn(AndroidSchedulers.mainThread())
          .switchMap(::showWeather)
          .onErrorResumeNext(::showError)
      }
      .doOnNext { debug("refreshWeather $it", TAG) }
  }

  private fun reduce(viewState: ViewState, partialStateChange: PartialStateChange): ViewState {
    return when (partialStateChange) {
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
        weather = partialStateChange.weather,
        showRefreshSuccessfully = partialStateChange.showMessage,
        error = null
      )
    }
  }

  private fun showError(throwable: Throwable): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialStateChange.Error(throwable = throwable, showMessage = false)
      }
      .startWith(
        PartialStateChange.Error(throwable = throwable, showMessage = true)
      )
      .cast()
  }

  private fun showWeather(currentWeather: CurrentWeather): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialStateChange.RefreshWeatherSuccess(
          showMessage = false,
          weather = currentWeather
        )
      }
      .startWith(
        PartialStateChange.RefreshWeatherSuccess(
          showMessage = true,
          weather = currentWeather
        )
      )
      .cast()
  }

  companion object {
    private val LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
  }
}