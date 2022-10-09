package com.hoc.weatherapp.ui.cities

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.City
import io.reactivex.Observable

interface CitiesContract {
  sealed class PartialStateChange {
    data class CityListItems(val items: List<CityListItem>) : PartialStateChange()

    data class Error(
      val throwable: Throwable,
      val showMessage: Boolean
    ) : PartialStateChange()

    data class DeleteCity(
      val showMessage: Boolean,
      val deletedCity: City
    ) : PartialStateChange()

    data class RefreshWeather(
      val showMessage: Boolean,
      val refreshCity: City
    ) : PartialStateChange()
  }

  data class ViewState(
    val cityListItems: List<CityListItem> = emptyList(),
    val error: Throwable? = null,
    val showError: Boolean = false,
    val showDeleteCitySuccessfully: Boolean = false,
    val deletedCity: City? = null,
    val showRefreshSuccessfully: Boolean = false,
    val refreshCity: City? = null
  )

  sealed class SearchStringIntent {
    abstract val value: String

    object InitialSearchStringIntent : SearchStringIntent() {
      override val value = ""
    }

    data class UserSearchStringIntent(override val value: String) : SearchStringIntent()
  }

  interface View : MvpView {
    fun searchStringIntent(): Observable<SearchStringIntent>

    fun changeSelectedCity(): Observable<City>

    fun deleteCityAtPosition(): Observable<Int>

    fun refreshCurrentWeatherAtPosition(): Observable<Int>

    fun render(state: ViewState)
  }
}
