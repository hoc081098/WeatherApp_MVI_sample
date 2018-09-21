package com.hoc.weatherapp.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.utils.debug
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class WeatherDao {
    @Query("SELECT * FROM current_weathers WHERE id = :id ORDER BY dataTime DESC LIMIT 1")
    abstract fun getCurrentWeatherById(id: Long): Flowable<CurrentWeather>

    @Insert(onConflict = OnConflictStrategy.FAIL)
    abstract fun insertCurrentWeather(weather: CurrentWeather)

    @Update
    abstract fun updateCurrentWeather(weather: CurrentWeather)

    @Query("DELETE FROM current_weathers WHERE id = :id")
    abstract fun deleteWeatherById(id: Long)

    @Query("SELECT * FROM current_weathers ORDER BY name")
    abstract fun getAllWeathers(): Flowable<List<CurrentWeather>>

    fun upsert(weather: CurrentWeather) {
        try {
            insertCurrentWeather(weather)
        } catch (e: SQLiteConstraintException) {
            debug(e, "WeatherDao")
            updateCurrentWeather(weather)
        }
    }

    @Query("SELECT * FROM current_weathers WHERE id = :id ORDER BY dataTime DESC LIMIT 1")
    abstract fun getCurrentWeatherByIdSingle(id: Long): Single<CurrentWeather>

    /*@Query("SELECT COUNT(*) FROM weathers")
    abstract fun getCountCity(): Flowable<Int>*/
}