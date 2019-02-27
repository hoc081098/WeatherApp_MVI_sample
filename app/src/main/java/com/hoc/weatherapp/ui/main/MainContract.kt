package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import io.reactivex.Observable

interface MainContract {
  sealed class ViewState {
    data class CityAndWeather(
      val city: City,
      val weather: CurrentWeather
    ) : ViewState()

    object NoSelectedCity : ViewState()
  }

  interface View : MvpView {
    fun changeVibrantColorIntent(): Observable<Int>

    fun render(state: ViewState)
  }
}