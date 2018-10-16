package com.hoc.weatherapp.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.FAIL
import androidx.room.Query
import androidx.room.Update
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.debug
import io.reactivex.Flowable

@Dao
abstract class CityDao {
    @Insert(onConflict = FAIL)
    abstract fun insertCity(city: City)

    @Query("SELECT * FROM cities WHERE id = :id")
    abstract fun getCityById(id: Long): Flowable<City>

    @Update
    abstract fun updateCity(city: City)

    @Delete
    abstract fun deleteCity(city: City)

    open fun upsert(city: City) {
        try {
            debug("Insert city=$city", "@@@")
            insertCity(city)
        } catch (e: SQLiteConstraintException) {
            debug("Insert fail --> update city=$city", "@@@")
            updateCity(city)
        }
    }
}