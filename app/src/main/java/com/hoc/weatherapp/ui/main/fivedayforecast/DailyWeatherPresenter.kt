package com.hoc.weatherapp.ui.main.fivedayforecast

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.PressureUnit
import com.hoc.weatherapp.data.models.SpeedUnit
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.WindDirection
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.*
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.PartialStateChange.Weather
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.work.WorkerUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import java.util.concurrent.TimeUnit

data class Tuple4(
  val weathers: List<DailyWeather>,
  val temperatureUnit: TemperatureUnit,
  val speedUnit: SpeedUnit,
  val pressureUnit: PressureUnit
)

class DailyWeatherPresenter(
  private val repository: Repository,
  private val sharedPrefUtil: SharedPrefUtil,
  private val androidApplication: Application
) :
  MviBasePresenter<View, ViewState>() {
  private val tag = "_five_day_forecast_"

  private fun mapListDailyWeathersToListItem(tuple4: Tuple4): List<DailyWeatherListItem> {
    val (weathers, temperatureUnit, windSpeedUnit, pressureUnit) = tuple4

    return weathers
      .groupBy { it.timeOfDataForecasted.trim() }
      .toSortedMap()
      .flatMap { (date, weathers) ->
        listOf(DailyWeatherListItem.Header(date)) +
            weathers.map {
              DailyWeatherListItem.Weather(
                weatherIcon = it.icon,
                main = it.main,
                weatherDescription = it.description.capitalize(),
                temperatureMin = temperatureUnit.format(it.temperatureMin),
                temperatureMax = temperatureUnit.format(it.temperatureMax),
                temperature = temperatureUnit.format(it.temperature),
                dataTime = it.timeOfDataForecasted,
                cloudiness = "${it.cloudiness}%",
                humidity = "${it.humidity}%",
                rainVolumeForTheLast3Hours = "${it.rainVolumeForTheLast3Hours}mm",
                snowVolumeForTheLast3Hours = "${it.snowVolumeForTheLast3Hours}mm",
                windDirection = WindDirection.fromDegrees(it.winDegrees),
                groundLevel = pressureUnit.format(it.groundLevel),
                pressure = pressureUnit.format(it.pressure),
                seaLevel = pressureUnit.format(it.seaLevel),
                winSpeed = windSpeedUnit.format(it.windSpeed)
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
          shared.ofType<RefreshIntent.InitialRefreshIntent>().take(1),
          shared.notOfType<RefreshIntent.InitialRefreshIntent>()
        )
      }
      .doOnNext { debug("refreshDailyWeatherIntent $it", "_daily_weather_") }
      .switchMap {
        repository.refreshFiveDayForecastOfSelectedCity()
          .doOnSuccess {
            WorkerUtil.enqueueUpdateDailyWeatherWorkWorkRequest()
          }
          .doOnError {
            if (it is NoSelectedCityException) {
              androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
              WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
            }
          }
          .toObservable()
          .map {
            mapListDailyWeathersToListItem(
              Tuple4(
                it,
                sharedPrefUtil.temperatureUnit,
                sharedPrefUtil.speedUnit,
                sharedPrefUtil.pressureUnit
              )
            )
          }
          .observeOn(AndroidSchedulers.mainThread())
          .switchMap(::showList)
          .onErrorResumeNext(::showError)
      }
  }

  private fun weatherChangePartialState(): Observable<PartialStateChange> {
    return Observables.combineLatest(
      source1 = repository.getFiveDayForecastOfSelectedCity()
        .map { it.getOrNull().orEmpty() },
      source2 = sharedPrefUtil.temperatureUnitObservable,
      source3 = sharedPrefUtil.speedUnitObservable,
      source4 = sharedPrefUtil.pressureUnitObservable,
      combineFunction = { list, temperatureUnit, speedUnit, pressureUnit ->
        Tuple4(list, temperatureUnit, speedUnit, pressureUnit)
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
