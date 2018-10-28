package com.hoc.weatherapp.ui.cities

import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.local.SharedPrefUtil
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.cities.CitiesContract.PartialStateChange
import com.hoc.weatherapp.ui.cities.CitiesContract.PartialStateChange.CityListItems
import com.hoc.weatherapp.ui.cities.CitiesContract.PartialStateChange.Error
import com.hoc.weatherapp.ui.cities.CitiesContract.SearchStringIntent.InitialSearchStringIntent
import com.hoc.weatherapp.ui.cities.CitiesContract.SearchStringIntent.UserSearchStringIntent
import com.hoc.weatherapp.ui.cities.CitiesContract.View
import com.hoc.weatherapp.ui.cities.CitiesContract.ViewState
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.notOfType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*
import io.reactivex.rxkotlin.Observables.combineLatest
import java.util.concurrent.TimeUnit

const val TAG = "cities"

class CitiesPresenter(
  private val repository: Repository,
  private val sharedPrefUtil: SharedPrefUtil
) : MviBasePresenter<View, ViewState>() {
  private val compositeDisposable = CompositeDisposable()

  override fun bindIntents() {
    val cityAndCurrentWeathers = intent(View::searchStringIntent)
      .publish { shared->
        Observable.mergeArray(
          shared.ofType<InitialSearchStringIntent>().take(1),
          shared.notOfType<InitialSearchStringIntent>()
        )
      }
      .cast<CitiesContract.SearchStringIntent>()
      .map { it.value }
      .doOnNext { debug("searchStringIntent '$it'", TAG) }
      .switchMap(repository::getAllCityAndCurrentWeathers)

    val cityListItemsPartialChange = cityListItemsPartialChange(cityAndCurrentWeathers)
    val deleteCityPartialChange = deleteCityPartialChange(cityAndCurrentWeathers)
    changeSelectedCity()
    refreshWeather(cityAndCurrentWeathers)

    subscribeViewState(Observable.mergeArray(cityListItemsPartialChange, deleteCityPartialChange)
      .scan(ViewState(), ::reduce)
      .distinctUntilChanged()
      .doOnNext { debug("CitiesPresenter ViewState = $it", TAG) }
      .observeOn(AndroidSchedulers.mainThread()), View::render)
  }

  private fun refreshWeather(cityAndCurrentWeathers: Observable<List<CityAndCurrentWeather>>) {
    intent { it.refreshCurrentWeatherAtPosition() }
      .filter { it != RecyclerView.NO_POSITION }
      .withLatestFrom(cityAndCurrentWeathers)
      .map { (position, list) -> list[position] }
      .map { it.city }
      .flatMap {
        repository
          .refreshWeatherOf(it)
          .toObservable<Unit>()
          .onErrorReturnItem(Unit)
      }
      .subscribe()
      .addTo(compositeDisposable)
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
          .toSingleDefault(it)
          .toObservable()
          .flatMap(::delete)
          .onErrorResumeNext(::showError)
      }
  }

  private fun changeSelectedCity() {
    intent(View::changeSelectedCity)
      .switchMap { city ->
        repository
          .changeSelectedCity(city)
          .andThen(
            repository
              .refreshCurrentWeather()
              .zipWith(repository.refreshFiveDayForecastOfSelectedCity())
          )
          .toObservable()
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
        error = null,
        deletedCity = null
      )
      is Error -> viewState.copy(
        showError = partialStateChange.showMessage,
        error = partialStateChange.throwable
      )
      is PartialStateChange.DeleteCity -> viewState.copy(
        showDeleteCitySuccessfully = partialStateChange.showMessage,
        deletedCity = partialStateChange.deletedCity
      )
    }
  }

  private fun delete(city: City): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map { PartialStateChange.DeleteCity(showMessage = false, deletedCity = city) }
      .startWith(PartialStateChange.DeleteCity(showMessage = true, deletedCity = city))
      .cast()
  }

  private fun showError(throwable: Throwable): Observable<PartialStateChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map { Error(showMessage = false, throwable = throwable) }
      .startWith(Error(showMessage = true, throwable = throwable))
      .cast()
  }
}

