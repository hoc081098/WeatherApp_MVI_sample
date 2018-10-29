package com.hoc.weatherapp.ui.cities

import android.app.Application
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.cities.CitiesContract.*
import com.hoc.weatherapp.ui.cities.CitiesContract.PartialStateChange.*
import com.hoc.weatherapp.ui.cities.CitiesContract.SearchStringIntent.InitialSearchStringIntent
import com.hoc.weatherapp.utils.*
import com.hoc.weatherapp.work.WorkerUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*
import io.reactivex.rxkotlin.Observables.combineLatest
import java.util.concurrent.TimeUnit

const val TAG = "cities"

class CitiesPresenter(
  private val repository: Repository,
  private val sharedPrefUtil: SharedPrefUtil,
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
      .switchMap(repository::getAllCityAndCurrentWeathers)

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
    return intent { it.refreshCurrentWeatherAtPosition() }
      .filter { it != RecyclerView.NO_POSITION }
      .withLatestFrom(cityAndCurrentWeathers)
      .map { (position, list) -> list[position] }
      .map { it.city }
      .flatMap { city ->
        repository
          .refreshWeatherOf(city)
          .doOnSuccess { (cityAndCurrentWeather) ->
            WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
            WorkerUtil.enqueueUpdateDailyWeatherWorkWorkRequest()
            if (sharedPrefUtil.showNotification) {
              androidApplication.showOrUpdateNotification(
                cityName = cityAndCurrentWeather.city.name,
                unit = sharedPrefUtil.temperatureUnit,
                cityCountry = cityAndCurrentWeather.city.country,
                weather = cityAndCurrentWeather.currentWeather
              )
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
    return intent { it.deleteCityAtPosition() }
      .filter { it != RecyclerView.NO_POSITION }
      .withLatestFrom(cityAndCurrentWeathers)
      .map { (position, list) -> list[position] }
      .map { it.city }
      .flatMap {
        repository
          .deleteCity(it)
          .doOnComplete {
            if (sharedPrefUtil.selectedCity === null) {
              androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
              WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
              WorkerUtil.cancelUpdateDailyWeatherWorkWorkRequest()
            }
          }
          .toSingleDefault(it)
          .toObservable()
          .observeOn(AndroidSchedulers.mainThread())
          .flatMap(::showDeleteResult)
          .onErrorResumeNext(::showError)
      }
  }

  private fun changeSelectedCity() {
    intent(View::changeSelectedCity)
      .switchMap { city ->
        repository
          .changeSelectedCity(city)
          .doOnComplete {
            repository
              .refreshCurrentWeatherOfSelectedCity()
              .zipWith(repository.refreshFiveDayForecastOfSelectedCity())
              .doOnSuccess { (cityAndCurrentWeather) ->
                WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
                WorkerUtil.enqueueUpdateDailyWeatherWorkWorkRequest()
                if (sharedPrefUtil.showNotification) {
                  androidApplication.showOrUpdateNotification(
                    cityName = cityAndCurrentWeather.city.name,
                    unit = sharedPrefUtil.temperatureUnit,
                    cityCountry = cityAndCurrentWeather.city.country,
                    weather = cityAndCurrentWeather.currentWeather
                  )
                }
              }
              .subscribeBy(onError = {})
              .addTo(compositeDisposable)
          }
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
    return combineLatest(repository.getSelectedCity(), cityAndCurrentWeathers)
      .map { (city, list) ->
        list.map {
          CityListItem(
            city = it.city,
            weatherIcon = it.currentWeather.icon,
            weatherConditionId = it.currentWeather.weatherConditionId,
            isSelected = it.city == city.getOrNull(),
            temperatureMax = sharedPrefUtil.temperatureUnit.format(it.currentWeather.temperatureMax),
            temperatureMin = sharedPrefUtil.temperatureUnit.format(it.currentWeather.temperatureMin),
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

