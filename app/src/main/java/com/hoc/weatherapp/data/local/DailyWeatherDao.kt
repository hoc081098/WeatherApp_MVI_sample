package com.hoc.weatherapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hoc.weatherapp.data.models.entity.DailyWeather
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class DailyWeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDailyWeathers(weathers: List<DailyWeather>)

    @Query("DELETE FROM five_day_forecast WHERE id = :id")
    abstract fun deleteAllDailyWeathersByCityId(id: Long)

    @Query("SELECT * FROM five_day_forecast WHERE id = :id ORDER BY timeOfDataForecasted")
    abstract fun getAllDailyWeathersByCityId(id: Long): Flowable<List<DailyWeather>>

    @Transaction
    open fun deleteDailyWeathersByCityIdAndInsert(
        id: Long,
        weathers: List<DailyWeather>
    ) {
        deleteAllDailyWeathersByCityId(id)
        insertDailyWeathers(weathers)
    }

    @Query("SELECT * FROM five_day_forecast WHERE id = :id ORDER BY timeOfDataForecasted")
    abstract fun getAllDailyWeathersByCityIdSingle(id: Long): Single<List<DailyWeather>>
}