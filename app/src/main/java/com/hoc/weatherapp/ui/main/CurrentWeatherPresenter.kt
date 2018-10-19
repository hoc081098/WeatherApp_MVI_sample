package com.hoc.weatherapp.ui.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.ui.main.CurrentWeatherContract.View
import com.hoc.weatherapp.ui.main.CurrentWeatherContract.ViewState
import com.hoc.weatherapp.ui.main.CurrentWeatherContract.ViewState.*
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.ofType
import java.util.concurrent.TimeUnit

private const val TAG = "^^&"

class CurrentWeatherPresenter(private val repository: Repository) :
    MviBasePresenter<View, ViewState>() {
    override fun bindIntents() {
        val refresh = intent(View::refreshCurrentWeatherIntent)
            .switchMap<ViewState> {
                repository.refreshCurrentWeather()
                    .toObservable()
                    .switchMap<ViewState>(::weather)
                    .onErrorResumeNext(::error)
            }
            .doOnNext { debug("refreshWeather $it", TAG) }

        val cityAndWeather = repository.getCityAndCurrentWeatherByCity()
            .toObservable()
            .publish { shared ->
                Observable.mergeArray(
                    shared.ofType<None>().switchMap { error(NoSelectedCityException) },
                    shared.ofType<Some<CityAndCurrentWeather>>()
                        .map { it.value }
                        .map { it.currentWeather }
                        .map { Weather(weather = it, showMessage = false) }
                )
            }
            .doOnNext { debug("cityAndWeather $it", TAG) }

        subscribeViewState(
            Observable.mergeArray(refresh, cityAndWeather)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { debug("CurrentWeatherPresenter ViewState = $it", TAG) },
            View::render
        )
    }

    private fun error(throwable: Throwable): Observable<ViewState> {
        return Observable.timer(2_000, TimeUnit.MILLISECONDS)
            .map {
                when (throwable) {
                    is NoSelectedCityException -> NoSelectedCity(
                        showMessage = false
                    )
                    else -> Error(throwable = throwable, showMessage = false)
                }
            }
            .startWith(
                when (throwable) {
                    is NoSelectedCityException -> NoSelectedCity(
                        showMessage = true
                    )
                    else -> Error(throwable = throwable, showMessage = true)
                }
            )
    }

    private fun weather(cityAndCurrentWeather: CityAndCurrentWeather): Observable<Weather> {
        return Observable.timer(2_000, TimeUnit.MILLISECONDS)
            .map {
                Weather(
                    showMessage = false,
                    weather = cityAndCurrentWeather.currentWeather
                )
            }
            .startWith(
                Weather(
                    showMessage = true,
                    weather = cityAndCurrentWeather.currentWeather
                )
            )
    }
}