package com.hoc.weatherapp.ui.cities

import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface CitiesContract {
  sealed class ViewState {
    data class CityListItems(
      val cityListItems: List<CityListItem>
    ) : ViewState()

    data class Error(
      val throwable: Throwable,
      val showMessage: Boolean
    ) : ViewState()
  }

  interface View : MvpView {
    fun searchStringIntent(): Observable<String>

    fun render(state: ViewState)
  }
}