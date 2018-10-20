package com.hoc.weatherapp.data.local

import android.content.Context
import androidx.room.*
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import java.util.*

object Converters {
  @JvmStatic
  @TypeConverter
  fun fromTimestamp(value: Long?): Date? = value?.let(::Date)

  @JvmStatic
  @TypeConverter
  fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Database(
  entities = [CurrentWeather::class, City::class, DailyWeather::class],
  version = 1,
  exportSchema = false
)
@TypeConverters(value = [Converters::class])
abstract class AppDatabase : RoomDatabase() {
  abstract fun cityDao(): CityDao
  abstract fun weatherDao(): CurrentWeatherDao
  abstract fun dailyWeatherDao(): DailyWeatherDao

  companion object {
    private const val DATABASE_NAME = "WEATHER_APP_DB"

    fun getInstance(context: Context): AppDatabase {
      return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        DATABASE_NAME
      ).build()
    }
  }
}