package com.hoc.weatherapp.data

import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.Optional
import io.reactivex.Observable
import io.reactivex.Single

interface CurrentWeatherRepository {
  /**
   * Refresh both current weather and five day forecast of [city], get from api
   * @return [Single] emit result or error
   */
  fun refreshWeatherOf(city: City): Single<Pair<CityAndCurrentWeather, List<DailyWeather>>>

  /**
   * Refresh current weather of selected city, get from api
   * @return [Single] emit result or error ([NoSelectedCityException] when have no selected city)
   */
  fun refreshCurrentWeatherOfSelectedCity(): Single<CityAndCurrentWeather>

  /**
   * Get pair of selected city and current weather, get from local database
   * @return [Observable] that emits [Optional]s of [CityAndCurrentWeather], None when having no selected city
   */
  fun getSelectedCityAndCurrentWeatherOfSelectedCity(): Observable<Optional<CityAndCurrentWeather>>

  /**
   * Get all pair of city and current weather, get from local database
   * @return [Observable] that emits [List]s of [CityAndCurrentWeather]
   */
  fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>>
}
