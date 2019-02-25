package com.hoc.weatherapp.ui.cities

import android.app.Application
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.CityRepository
import com.hoc.weatherapp.data.CurrentWeatherRepository
import com.hoc.weatherapp.data.FiveDayForecastRepository
import com.hoc.weatherapp.data.local.SelectedCityPreference
import com.hoc.weatherapp.data.local.SettingPreferences
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.cities.CitiesContract.*
import com.hoc.weatherapp.ui.cities.CitiesContract.PartialStateChange.*
import com.hoc.weatherapp.ui.cities.CitiesContract.SearchStringIntent.InitialSearchStringIntent
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.worker.WorkerUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*
import io.reactivex.rxkotlin.Observables.combineLatest
import java.util.concurrent.TimeUnit

const val TAG = "cities"

class CitiesPresenter(
  private val cityRepository: CityRepository,
  private val currentWeatherRepository: CurrentWeatherRepository,
  private val fiveDayForecastRepository: FiveDayForecastRepository,
  private val settingPreferences: SettingPreferences,
  private val selectedCityPreference: SelectedCityPreference,
  private val androidApplication: Application
) : MviBasePresenter<View, ViewState>() {
  private val compositeDisposable = CompositeDisposable()

  override fun bindIntents() {
    val cityAndCurrentWeathers = intent(View::searchStringIntent)
      .publish { shared ->
        Observable.mergeArray(
          shared.ofType<InitialSearchStringIntent>().take(1),
          shared.notOfType<InitialSearchStringIntent>()
        )
      }
      .cast<CitiesContract.SearchStringIntent>()
      .map { it.value }
      .doOnNext { debug("searchStringIntent '$it'", TAG) }
      .switchMap(currentWeatherRepository::getAllCityAndCurrentWeathers)
      .share()

    changeSelectedCity()

    subscribeViewState(
      Observable.mergeArray(
        cityListItemsPartialChange(cityAndCurrentWeathers),
        deleteCityPartialChange(cityAndCurrentWeathers),
        refreshWeather(cityAndCurrentWeathers)
      ).scan(ViewState(), ::reduce)
        .distinctUntilChanged()
        .doOnNext { debug("CitiesPresenter ViewState = $it", TAG) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private fun refreshWeather(cityAndCurrentWeathers: Observable<List<CityAndCurrentWeather>>): Observable<PartialStateChange> {
    return intent(View::refreshCurrentWeatherAtPosition)
      .filter { it != RecyclerView.NO_POSITION }
      .withLatestFrom(cityAndCurrentWeathers)
      .map { (position, list) -> list[position] }
      .map { it.city }
      .flatMap { city ->
        currentWeatherRepository
          .refreshWeatherOf(city)
          .doOnSuccess { (cityAndCurrentWeather) ->
            /**
             * If refresh current selected city
             */
            if (city == selectedCityPreference.value.getOrNull()) {

              if (settingPreferences.autoUpdatePreference.value) {
                WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
                WorkerUtil.enqueueUpdateDailyWeatherWorkWorkRequest()
              }

              if (settingPreferences.showNotificationPreference.value) {
                androidApplication.showOrUpdateNotification(
                  cityName = cityAndCurrentWeather.city.name,
                  unit = settingPreferences.temperatureUnitPreference.value,
                  cityCountry = cityAndCurrentWeather.city.country,
                  weather = cityAndCurrentWeather.currentWeather,
                  popUpAndSound = settingPreferences.soundNotificationPreference.value
                )
              }
            }
          }
          .map { it.first.city }
          .toObservable()
          .observeOn(AndroidSchedulers.mainThread())
          .flatMap(::showRefreshResult)
          .onErrorResumeNext(::showError)
      }
  }

  private fun deleteCityPartialChange(cityAndCurrentWeathers: Observable<List<CityAndCurrentWeather>>): Observable<PartialStateChange> {
    return intent(View::deleteCityAtPosition)
      .filter { it != RecyclerView.NO_POSITION }
      .withLatestFrom(cityAndCurrentWeathers)
      .map { (position, list) -> list[position] }
      .map { it.city }
      .flatMap {
        cityRepository
          .deleteCity(it)
          .doOnSuccess {
            /**
             * If delete selected city
             */
            if (selectedCityPreference.value is None) {
              androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
              WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
              WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
            }
          }
          .toObservable()
          .observeOn(AndroidSchedulers.mainThread())
          .flatMap(::showDeleteResult)
          .onErrorResumeNext(::showError)
      }
  }

  private fun changeSelectedCity() {
    val getWeatherSingle = currentWeatherRepository
      .refreshCurrentWeatherOfSelectedCity()
      .zipWith(fiveDayForecastRepository.refreshFiveDayForecastOfSelectedCity())
      .doOnSuccess { (cityAndCurrentWeather) ->

        if (settingPreferences.autoUpdatePreference.value) {
          WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
          WorkerUtil.enqueueUpdateDailyWeatherWorkWorkRequest()
        }

        if (settingPreferences.showNotificationPreference.value) {
          androidApplication.showOrUpdateNotification(
            cityName = cityAndCurrentWeather.city.name,
            unit = settingPreferences.temperatureUnitPreference.value,
            cityCountry = cityAndCurrentWeather.city.country,
            weather = cityAndCurrentWeather.currentWeather,
            popUpAndSound = settingPreferences.soundNotificationPreference.value
          )
        }

      }

    intent(View::changeSelectedCity)
      .switchMap { city ->
        cityRepository
          .changeSelectedCity(city)
          .doOnComplete { getWeatherSingle.subscribeBy(onError = {}).addTo(compositeDisposable) }
          .toObservable<Unit>()
          .onErrorResumeNext(Observable.empty())
      }
      .subscribeBy(
        onNext = { debug("changeSelectedCity onNext=$it", TAG) },
        onError = { debug("changeSelectedCity onNext=$it", TAG) }
      )
      .addTo(compositeDisposable)
  }

  private fun cityListItemsPartialChange(cityAndCurrentWeathers: Observable<List<CityAndCurrentWeather>>): Observable<PartialStateChange> {
    return combineLatest(
      cityRepository.getSelectedCity(),
      cityAndCurrentWeathers,
      settingPreferences.temperatureUnitPreference.observable
    )
      .map { (city, list, temperatureUnit) ->
        list.map {
          CityListItem(
            city = it.city,
            weatherIcon = it.currentWeather.icon,
            weatherConditionId = it.currentWeather.weatherConditionId,
            isSelected = it.city == city.getOrNull(),
            temperatureMax = temperatureUnit.format(it.currentWeather.temperatureMax),
            temperatureMin = temperatureUnit.format(it.currentWeather.temperatureMin),
            weatherDescription = it.currentWeather.description
          )
        }
      }
      .map(::CityListItems)
      .cast<PartialStateChange>()
      .observeOn(AndroidSchedulers.mainThread())
      .onErrorResumeNext(::showError)
      .doOnNext { debug("cityListItems $it", TAG) }
  }

  override fun unbindIntents() {
    super.unbindIntents()
    compositeDisposable.clear()
  }

  private fun reduce(viewState: ViewState, partialStateChange: PartialStateChange): ViewState {
    return when (partialStateChange) {
      is CityListItems -> viewState.copy(
        cityListItems = partialStateChange.items,
        error = null
      )
      is Error -> viewState.copy(
        showError = partialStateChange.showMessage,
        error = partialStateChange.throwable
      )
      is DeleteCity -> viewState.copy(
        showDeleteCitySuccessfully = partialStateChange.showMessage,
        deletedCity = partialStateChange.deletedCity
      )
      is RefreshWeather -> viewState.copy(
        showRefreshSuccessfully = partialStateChange.showMessage,
        refreshCity = partialStateChange.refreshCity
      )
    }
  }

  private fun showRefreshResult(city: City): Observable<PartialStateChange> {
    return Observable.timer(SNACKBAR_DURATION, TimeUnit.MILLISECONDS)
      .map { RefreshWeather(showMessage = false, refreshCity = city) }
      .startWith(RefreshWeather(showMessage = true, refreshCity = city))
      .cast()
  }

  private fun showDeleteResult(city: City): Observable<PartialStateChange> {
    return Observable.timer(SNACKBAR_DURATION, TimeUnit.MILLISECONDS)
      .map { DeleteCity(showMessage = false, deletedCity = city) }
      .startWith(DeleteCity(showMessage = true, deletedCity = city))
      .cast()
  }

  private fun showError(throwable: Throwable): Observable<PartialStateChange> {
    return Observable.timer(SNACKBAR_DURATION, TimeUnit.MILLISECONDS)
      .map { Error(showMessage = false, throwable = throwable) }
      .startWith(Error(showMessage = true, throwable = throwable))
      .cast()
  }

  companion object {
    private const val SNACKBAR_DURATION = 2_000L
  }
}

