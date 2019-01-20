package com.hoc.weatherapp.ui.main.currentweather

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherContract.*
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.worker.WorkerUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
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

  private fun cityAndWeatherPartialChange(): Observable<PartialStateChange> {
    return currentWeatherRepository
      .getSelectedCityAndCurrentWeatherOfSelectedCity()
      .switchMap { optional ->
        when (optional) {
          is None -> showError(NoSelectedCityException)
          is Some -> PartialStateChange.Weather(
            weather = toCurrentWeather(
              optional.value.currentWeather,
              settingPreferences.speedUnitPreference.value,
              settingPreferences.pressureUnitPreference.value,
              settingPreferences.temperatureUnitPreference.value
            )
          ).let { Observable.just<PartialStateChange>(it) }.onErrorResumeNext(::showError)
        }
      }
      .doOnNext { debug("cityAndWeather $it", TAG) }
  }

  private fun toCurrentWeather(
    entity: com.hoc.weatherapp.data.models.entity.CurrentWeather,
    speedUnit: SpeedUnit,
    pressureUnit: PressureUnit,
    temperatureUnit: TemperatureUnit
  ): CurrentWeather {
    return CurrentWeather(
      temperature = UnitConverter.convertTemperature(entity.temperature, temperatureUnit),
      pressure = UnitConverter.convertPressure(entity.pressure, pressureUnit),
      rainVolumeForThe3HoursMm = entity.rainVolumeForThe3Hours,
      visibilityKm = entity.visibility / 1_000,
      humidity = entity.humidity,
      description = entity.description.capitalize(),
      dataTime = entity.dataTime,
      weatherConditionId = entity.weatherConditionId,
      weatherIcon = entity.icon,
      winSpeed = UnitConverter.convertSpeed(entity.winSpeed, speedUnit),
      winDegrees = entity.winDegrees
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
      .switchMap {
        currentWeatherRepository
          .refreshCurrentWeatherOfSelectedCity()
          .doOnSuccess {
            WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
            if (settingPreferences.showNotificationPreference.value) {
              androidApplication.showOrUpdateNotification(
                cityName = it.city.name,
                unit = settingPreferences.temperatureUnitPreference.value,
                cityCountry = it.city.country,
                weather = it.currentWeather
              )
            }
          }
          .doOnError {
            if (it is NoSelectedCityException) {
              androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
              WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
            }
          }
          .toObservable()
          .map {
            toCurrentWeather(
              it.currentWeather,
              settingPreferences.speedUnitPreference.value,
              settingPreferences.pressureUnitPreference.value,
              settingPreferences.temperatureUnitPreference.value
            )
          }
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
}