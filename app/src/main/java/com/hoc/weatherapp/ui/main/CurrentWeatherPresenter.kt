package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.NoSelectedCity
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.CurrentWeatherContract.View
import com.hoc.weatherapp.ui.main.CurrentWeatherContract.ViewState
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.ofType

private const val TAG = "^^&"

class CurrentWeatherPresenter(private val repository: Repository) :
    MviBasePresenter<View, ViewState>() {
    override fun bindIntents() {
        val refresh = intent { it.refreshCurrentWeatherIntent() }
            .switchMap<ViewState> {
                repository.refreshCurrentWeather()
                    .toObservable<ViewState>()
                    .materialize()
                    .map { notification ->
                        when {
                            notification.isOnComplete -> ViewState.refreshWeatherDone()
                            notification.isOnError -> when (val error = notification.error) {
                                is NoSelectedCity -> ViewState.hasNoSelectedCity()
                                else -> ViewState.error(error)
                            }
                            else -> throw IllegalStateException("Not handle notification::isOnNext")
                        }
                    }
            }
            .doOnNext { debug("refresh $it", TAG) }

        val cityAndWeather = repository.getCityAndCurrentWeatherByCity()
            .toObservable()
            .publish { shared ->
                Observable.mergeArray(
                    shared.ofType<None>().map { ViewState.hasNoSelectedCity() },
                    shared.ofType<Some<CityAndCurrentWeather>>()
                        .map { it.value }
                        .map<ViewState> { ViewState.cityAndWeather(it) }
                )
            }
            .doOnNext { debug("cityAndWeather $it", TAG) }

        subscribeViewState(
            Observable.mergeArray(
                refresh,
                cityAndWeather
            ).distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { debug("State = $it", TAG) },
            CurrentWeatherContract.View::render
        )
    }
}