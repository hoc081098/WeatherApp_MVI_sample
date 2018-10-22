package com.hoc.weatherapp.ui.cities

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.City
import io.reactivex.Observable

interface CitiesContract {
  sealed class PartialChange {
    data class CityListItems(val items: List<CityListItem>) : PartialChange()

    data class Error(
      val throwable: Throwable,
      val showMessage: Boolean
    ) : PartialChange()

    data class DeleteCity(
      val showMessage: Boolean,
      val deletedCity: City
    ): PartialChange()
  }

  data class ViewState(
    val cityListItems: List<CityListItem> = emptyList(),
    val error: Throwable? = null,
    val showError: Boolean = false,
    val showDeleteCitySuccessfully: Boolean = false,
    val deletedCity: City? = null
  )

  interface View : MvpView {
    fun searchStringIntent(): Observable<String>

    fun changeSelectedCity(): Observable<City>

    fun deleteCityAtPosition(): Observable<Int>

    fun refreshCurrentWeatherAtPosition(): Observable<Int>

    fun render(state: ViewState)
  }
}