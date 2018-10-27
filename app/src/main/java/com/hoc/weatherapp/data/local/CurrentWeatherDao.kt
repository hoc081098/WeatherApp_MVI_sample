package com.hoc.weatherapp.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.models.entity.CurrentWeather
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

  @Insert(onConflict = OnConflictStrategy.FAIL)
  abstract fun insertCurrentWeather(currentWeather: CurrentWeather)

  @Update
  abstract fun updateCurrentWeather(currentWeather: CurrentWeather)

  @Transaction
  open fun upsert(currentWeather: CurrentWeather) {
    try {
      insertCurrentWeather(currentWeather)
    } catch (e: SQLiteConstraintException) {
      updateCurrentWeather(currentWeather)
    }
  }
}