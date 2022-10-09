package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.Optional
import io.reactivex.Observable
import io.reactivex.Single

interface FiveDayForecastRepository {
  /**
   * Get stream of five day weather, get from local database
   * @return [Observable] emit [None] when having no selected city, otherwise emit [Some] of [DailyWeather]s with [City]
   */
  fun getFiveDayForecastOfSelectedCity(): Observable<Optional<Pair<City, List<DailyWeather>>>>

  /**
   * Refresh five day forecast of selected city, get from api
   * @return [Single] emit result or error, emit [NoSelectedCityException] when having no selected city
   */
  fun refreshFiveDayForecastOfSelectedCity(): Single<Pair<City, List<DailyWeather>>>
}
