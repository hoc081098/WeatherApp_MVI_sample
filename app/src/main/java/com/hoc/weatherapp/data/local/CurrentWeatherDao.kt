package com.hoc.weatherapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.utils.debug
import io.reactivex.Observable

@Dao
abstract class CurrentWeatherDao {
  @Query(
    """SELECT * FROM current_weathers INNER JOIN cities ON current_weathers.city_id = cities.id
                WHERE city_id = :cityId LIMIT 1"""
  )
  abstract fun getCityAndCurrentWeatherByCityId(cityId: Long): Observable<CityAndCurrentWeather>

  @Query(
    """SELECT * FROM current_weathers INNER JOIN cities ON current_weathers.city_id = cities.id
            WHERE cities.name LIKE '%' || :querySearch || '%'
                   OR cities.country LIKE '%' || :querySearch || '%'
                   OR current_weathers.description LIKE '%' || :querySearch || '%'
                   OR current_weathers.main LIKE '%' || :querySearch || '%'
            ORDER BY city_id"""
  )
  abstract fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  abstract fun insertCurrentWeather(currentWeather: CurrentWeather): Long

  @Update
  abstract fun updateCurrentWeather(currentWeather: CurrentWeather)

  @Transaction
  open fun upsert(weather: CurrentWeather) {
    insertCurrentWeather(weather)
      .takeIf {
        debug("insertCurrentWeather => $it", "__DAO__")
        it == -1L
      }
      ?.let { updateCurrentWeather(weather) }
  }
}
