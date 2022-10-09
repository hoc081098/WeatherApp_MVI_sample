package com.hoc.weatherapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Transaction
import androidx.room.Update
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.debug

@Dao
abstract class CityDao {
  @Insert(onConflict = IGNORE)
  abstract fun insertCity(city: City): Long

  @Update
  abstract fun updateCity(city: City)

  @Delete
  abstract fun deleteCity(city: City)

  @Transaction
  open fun upsert(city: City) {
    insertCity(city)
      .takeIf {
        debug("insertCity => $it", "__DAO__")
        it == -1L
      }
      ?.let { updateCity(city) }
  }
}
