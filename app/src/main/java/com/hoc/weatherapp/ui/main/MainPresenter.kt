package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.MainContract.ViewState.*
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.ofType

class MainPresenter(private val repository: Repository) :
    MviBasePresenter<MainContract.View, MainContract.ViewState>() {
    override fun bindIntents() {
        val cityAndCurrentWeather = repository.getCityAndCurrentWeatherByCity()
            .toObservable()
            .publish { shared ->
                Observable.merge(
                    shared.ofType<Some<CityAndCurrentWeather>>()
                        .map { it.value }
                        .map {
                            CityAndWeather(
                                it.city,
                                it.currentWeather
                            )
                        },
                    shared.ofType<None>().map { NoSelectedCity }
                )
            }
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())

        subscribeViewState(cityAndCurrentWeather, MainContract.View::render)
    }
}