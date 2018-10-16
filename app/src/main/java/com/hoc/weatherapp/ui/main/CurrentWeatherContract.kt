package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvp.MvpView
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import io.reactivex.Observable

interface CurrentWeatherContract {
    data class ViewState(
        val weather: CurrentWeather?,
        val isLoading: Boolean,
        val error: Throwable?,
        val hasSelectedCity: Boolean
    ) {
        companion object {
            @JvmStatic
            fun refreshWeatherDone() = ViewState(
                isLoading = false,
                error = null,
                weather = null,
                hasSelectedCity = true
            )

            @JvmStatic
            fun loading() = ViewState(
                isLoading = true,
                weather = null,
                hasSelectedCity = true,
                error = null
            )

            @JvmStatic
            fun cityAndWeather(cityAndCurrentWeather: CityAndCurrentWeather) = ViewState(
                isLoading = false,
                weather = cityAndCurrentWeather.currentWeather,
                error = null,
                hasSelectedCity = true
            )

            @JvmStatic
            fun hasNoSelectedCity() = ViewState(
                isLoading = false,
                weather = null,
                error = null,
                hasSelectedCity = false
            )

            @JvmStatic
            fun error(throwable: Throwable?) = ViewState(
                isLoading = false,
                weather = null,
                error = throwable,
                hasSelectedCity = true
            )
        }
    }

    interface View : MvpView {
        fun refreshCurrentWeatherIntent(): Observable<Unit>

        fun render(state: ViewState)
    }
}