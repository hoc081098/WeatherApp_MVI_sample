package com.hoc.weatherapp.data

import android.content.Context
import androidx.room.*
import java.util.*

object Converters {
    @JvmStatic
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let(::Date)

    @JvmStatic
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Database(entities = [Weather::class, City::class], version = 1, exportSchema = false)
@TypeConverters(value = [Converters::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        private const val DATABASE_NAME = "DATABASE_NAME"

        fun getInstance(context: Context): AppDatabase {
            return Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    DATABASE_NAME
            ).build()
        }
    }
}