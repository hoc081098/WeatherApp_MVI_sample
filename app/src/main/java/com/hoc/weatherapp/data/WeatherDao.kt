package com.hoc.weatherapp.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.*
import com.hoc.weatherapp.utils.debug
import io.reactivex.Flowable

@Dao
abstract class WeatherDao {
    @Query("SELECT * FROM weathers WHERE id = :id ORDER BY dataTime DESC LIMIT 1")
    abstract fun getCurrentWeatherById(id: Long): Flowable<Weather>

    @Insert(onConflict = OnConflictStrategy.FAIL)
    abstract fun insertCurrentWeather(weather: Weather)

    @Update
    abstract fun updateCurrentWeather(weather: Weather)

    @Delete
    abstract fun deleteWeather(weather: Weather)

    @Query("SELECT * FROM weathers ORDER BY name")
    abstract fun getAllWeathers(): Flowable<List<Weather>>

    fun upsert(weather: Weather) {
        try {
            insertCurrentWeather(weather)
        } catch (e: SQLiteConstraintException) {
            debug(e, "WeatherDao")
            updateCurrentWeather(weather)
        }
    }

    /*@Query("SELECT COUNT(*) FROM weathers")
    abstract fun getCountCity(): Flowable<Int>*/
}