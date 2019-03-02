package com.hoc.weatherapp.ui.main.fivedayforecast

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CityRepository
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.WindDirection
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.ui.main.ColorHolderSource
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.*
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.PartialStateChange.Weather
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.worker.WorkerUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import java.util.concurrent.TimeUnit

class DailyWeatherPresenter(
  private val fiveDayForecastRepository: FiveDayForecastRepository,
  private val cityRepository: CityRepository,
  private val settingPreferences: SettingPreferences,
  private val colorHolderSource: ColorHolderSource,
  private val androidApplication: Application
) :
  MviBasePresenter<View, ViewState>() {
  private val tag = "_five_day_forecast_"

  private data class Tuple5(
    val weathers: Pair<City, List<DailyWeather>>,
    val temperatureUnit: TemperatureUnit,
    val speedUnit: SpeedUnit,
    val pressureUnit: PressureUnit,
    val iconBackgroundColor: Int
  )

  private fun mapListDailyWeathersToListItem(tuple5: Tuple5): List<DailyWeatherListItem> {
    val (cityAndWeathers, temperatureUnit, windSpeedUnit, pressureUnit, iconBackgroundColor) = tuple5

    return cityAndWeathers
      .second
      .groupBy { it.timeOfDataForecasted.trim() }
      .toSortedMap()
      .flatMap { (date, weathers) ->
        val zoneId = cityAndWeathers.first.zoneId

        listOf(DailyWeatherListItem.Header(date.toZonedDateTime(zoneId))) +
          weathers.map {
            DailyWeatherListItem.Weather(
              weatherIcon = it.icon,
              main = it.main,
              weatherDescription = it.description.capitalize(),
              temperatureMin = temperatureUnit.format(it.temperatureMin),
              temperatureMax = temperatureUnit.format(it.temperatureMax),
              temperature = temperatureUnit.format(it.temperature),
              dataTime = it.timeOfDataForecasted.toZonedDateTime(zoneId),
              cloudiness = "${it.cloudiness}%",
              humidity = "${it.humidity}%",
              rainVolumeForTheLast3Hours = "${it.rainVolumeForTheLast3Hours}mm",
              snowVolumeForTheLast3Hours = "${it.snowVolumeForTheLast3Hours}mm",
              windDirection = WindDirection.fromDegrees(it.winDegrees),
              groundLevel = pressureUnit.format(it.groundLevel),
              pressure = pressureUnit.format(it.pressure),
              seaLevel = pressureUnit.format(it.seaLevel),
              winSpeed = windSpeedUnit.format(it.windSpeed),
              iconBackgroundColor = iconBackgroundColor
            )
          }
      }
  }

  override fun bindIntents() {
    subscribeViewState(
      Observable.mergeArray(weatherChangePartialState(), refreshWeatherPartialChange())
        .scan(ViewState(), ::reduce)
        .distinctUntilChanged()
        .doOnNext { debug("ViewState=$it", tag) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private fun refreshWeatherPartialChange(): Observable<PartialStateChange> {
    return intent { it.refreshDailyWeatherIntent() }
      .publish { shared ->
        Observable.mergeArray(
          shared.ofType<RefreshIntent.InitialRefreshIntent>()
            .take(1)
            .delay { cityRepository.getSelectedCity().filter { it is Some } },
          shared.notOfType<RefreshIntent.InitialRefreshIntent>()
        )
      }
      .doOnNext { debug("refreshDailyWeatherIntent $it", "_daily_weather_") }
      .exhaustMap {
        fiveDayForecastRepository
          .refreshFiveDayForecastOfSelectedCity()
          .doOnSuccess {

            if (settingPreferences.autoUpdatePreference.value) {
              WorkerUtil.enqueueUpdateDailyWeatherWorkWorkRequest()
            }

          }
          .doOnError {
            if (it is NoSelectedCityException) {
              androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
              WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
              WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
            }
          }
          .toObservable()
          .observeOn(AndroidSchedulers.mainThread())
          .map {
            mapListDailyWeathersToListItem(
              Tuple5(
                it,
                settingPreferences.temperatureUnitPreference.value,
                settingPreferences.speedUnitPreference.value,
                settingPreferences.pressureUnitPreference.value,
                colorHolderSource.vibrantColor.also { debug("refresh vibrantColor=$it", tag) }
              )
            )
          }
          .switchMap(::showList)
          .onErrorResumeNext(::showError)
      }
  }

  private fun weatherChangePartialState(): Observable<PartialStateChange> {
    return Observables.combineLatest(
      source1 = fiveDayForecastRepository.getFiveDayForecastOfSelectedCity()
        .ofType<Some<Pair<City, List<DailyWeather>>>>()
        .map { it.value },
      source2 = settingPreferences.temperatureUnitPreference.observable,
      source3 = settingPreferences.speedUnitPreference.observable,
      source4 = settingPreferences.pressureUnitPreference.observable,
      source5 = colorHolderSource.vibrantColorObservable.doOnNext {
        debug(
          "combine vibrantColor=$it",
          tag
        )
      },
      combineFunction = { list, temperatureUnit, speedUnit, pressureUnit, color ->
        Tuple5(list, temperatureUnit, speedUnit, pressureUnit, color)
      }
    ).map(::mapListDailyWeathersToListItem)
      .map(::Weather)
      .cast<PartialStateChange>()
      .onErrorResumeNext(::showError)
  }

  private fun showList(list: List<DailyWeatherListItem>): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialStateChange.RefreshWeatherSuccess(
          showMessage = false,
          dailyWeatherListItem = list
        )
      }
      .startWith(
        PartialStateChange.RefreshWeatherSuccess(
          showMessage = true,
          dailyWeatherListItem = list
        )
      )
      .cast()
  }

  private fun showError(throwable: Throwable): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map {
        PartialStateChange.Error(
          showMessage = false,
          throwable = throwable
        )
      }
      .startWith(
        PartialStateChange.Error(
          showMessage = true,
          throwable = throwable
        )
      )
      .cast()
  }

  private fun reduce(viewState: ViewState, partialStateChange: PartialStateChange): ViewState {
    return when (partialStateChange) {
      is DailyWeatherContract.PartialStateChange.Error -> viewState.copy(
        showError = partialStateChange.showMessage,
        error = partialStateChange.throwable
      )
      is DailyWeatherContract.PartialStateChange.Weather -> viewState.copy(
        dailyWeatherListItem = partialStateChange.dailyWeatherListItem,
        error = null
      )
      is DailyWeatherContract.PartialStateChange.RefreshWeatherSuccess -> viewState.copy(
        dailyWeatherListItem = partialStateChange.dailyWeatherListItem,
        showRefreshSuccessfully = partialStateChange.showMessage,
        error = null
      )
    }
  }
}
