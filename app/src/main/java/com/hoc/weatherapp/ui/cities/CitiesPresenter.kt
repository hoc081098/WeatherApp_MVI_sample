package com.hoc.weatherapp.ui.cities

import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.cities.CitiesContract.*
import com.hoc.weatherapp.ui.cities.CitiesContract.PartialChange.CityListItems
import com.hoc.weatherapp.ui.cities.CitiesContract.PartialChange.Error
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.toOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*
import io.reactivex.rxkotlin.Observables.combineLatest
import java.util.concurrent.TimeUnit

const val TAG = "&^&"

class CitiesPresenter(private val repository: Repository) : MviBasePresenter<View, ViewState>() {
  private val compositeDisposable = CompositeDisposable()

  override fun bindIntents() {
    val cityAndCurrentWeathers = intent(View::searchStringIntent)
      .switchMap {
        repository.getAllCityAndCurrentWeathers(it).toObservable()
      }

    val cityListItemsPartialChange = cityListItemsPartialChange(cityAndCurrentWeathers)
    val deleteCityPartialChange = deleteCityPartialChange(cityAndCurrentWeathers)
    changeSelectedCity()

    subscribeViewState(Observable.mergeArray(cityListItemsPartialChange, deleteCityPartialChange)
      .scan(ViewState(), ::reduce)
      .distinctUntilChanged()
      .doOnNext { debug("CitiesPresenter ViewState = $it", TAG) }
      .observeOn(AndroidSchedulers.mainThread()), View::render)
  }

  private fun deleteCityPartialChange(cityAndCurrentWeathers: Observable<List<CityAndCurrentWeather>>): Observable<PartialChange> {
    return intent { it.deleteCityAtPosition() }
      .filter { it != RecyclerView.NO_POSITION }
      .withLatestFrom(cityAndCurrentWeathers)
      .map { (position, list) -> list.getOrNull(position).toOptional() }
      .ofType<Some<CityAndCurrentWeather>>()
      .map { it.value }
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
          .toSingleDefault(Result.success("changeSelectedCity $city successfully"))
          .flatMap {
            repository
              .refreshCurrentWeather()
              .map { Result.success("changeSelectedCity and refreshCurrentWeather $city successfully") }
          }
          .onErrorReturn { Result.failure(it) }
          .toObservable()
      }
      .subscribeBy(onNext = { debug("changeSelectedCity onNext=$it", TAG) })
      .addTo(compositeDisposable)
  }

  private fun cityListItemsPartialChange(cityAndCurrentWeathers: Observable<List<CityAndCurrentWeather>>): Observable<PartialChange> {
    return combineLatest(repository.getSelectedCity().toObservable(), cityAndCurrentWeathers)
      .map { (city, list) ->
        list.map {
          CityListItem(
            city = it.city,
            weatherIcon = it.currentWeather.icon,
            weatherConditionId = it.currentWeather.weatherConditionId,
            isSelected = it.city.id == city.getOrNull()?.id,
            temperatureMax = it.currentWeather.temperatureMax.toString(),
            temperatureMin = it.currentWeather.temperatureMin.toString(),
            weatherDescription = it.currentWeather.description
          )
        }
      }
      .map(::CityListItems)
      .cast<PartialChange>()
      .onErrorResumeNext(::showError)
      .doOnNext { debug("cityListItems $it", TAG) }
  }

  override fun unbindIntents() {
    super.unbindIntents()
    compositeDisposable.clear()
  }

  private fun reduce(viewState: ViewState, partialChange: PartialChange): ViewState {
    return when (partialChange) {
      is CityListItems -> viewState.copy(
        cityListItems = partialChange.items,
        error = null
      )
      is Error -> viewState.copy(
        showError = partialChange.showMessage,
        error = partialChange.throwable
      )
      is PartialChange.DeleteCity -> viewState.copy(
        showDeleteCitySuccessfully = partialChange.showMessage,
        deletedCity = partialChange.deletedCity
      )
    }
  }

  private fun delete(city: City): Observable<PartialChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map { PartialChange.DeleteCity(showMessage = false, deletedCity = city) }
      .startWith(PartialChange.DeleteCity(showMessage = true, deletedCity = city))
      .cast()
  }

  private fun showError(throwable: Throwable): Observable<PartialChange> {
    return Observable.timer(2_000, TimeUnit.MILLISECONDS)
      .map { Error(showMessage = false, throwable = throwable) }
      .startWith(Error(showMessage = true, throwable = throwable))
      .cast()
  }
}

