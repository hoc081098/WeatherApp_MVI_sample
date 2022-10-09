package com.hoc.weatherapp.ui.addcity

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.City
import io.reactivex.Observable

interface AddCityContract {
  sealed class ViewState {
    object Loading : ViewState()

    data class AddCitySuccessfully(val city: City, val showMessage: Boolean) : ViewState()

    data class Error(val throwable: Throwable, val showMessage: Boolean) : ViewState()
  }

  interface View : MvpView {
    fun addCurrentLocationIntent(): Observable<Unit>

    fun addCityByLatLngIntent(): Observable<Pair<Double, Double>>

    fun render(state: ViewState)
  }
}
