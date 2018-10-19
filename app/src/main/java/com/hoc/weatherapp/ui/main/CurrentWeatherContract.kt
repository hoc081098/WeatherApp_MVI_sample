package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import io.reactivex.Observable

interface CurrentWeatherContract {
    sealed class ViewState {
        object Loading : ViewState()

        data class NoSelectedCity(val showMessage: Boolean) : ViewState()

        data class Weather(val weather: CurrentWeather, val showMessage: Boolean) : ViewState()

        data class Error(val throwable: Throwable, val showMessage: Boolean) : ViewState()
    }

    interface View : MvpView {
        fun refreshCurrentWeatherIntent(): Observable<Unit>

        fun render(state: ViewState)
    }
}