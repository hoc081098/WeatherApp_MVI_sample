package com.hoc.weatherapp.data.models.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.parcelize.Parcelize

/**
 * Declaring the column info allows for the renaming of variables without implementing a
 * database migration, as the column name would not change.
 */

@Parcelize
@Entity(
  tableName = "current_weathers",
  foreignKeys = [
    ForeignKey(
      entity = City::class,
      onDelete = CASCADE,
      parentColumns = ["id"],
      childColumns = ["city_id"]
    )
  ]
)
data class CurrentWeather(
  /**
   * Id of city
   */
  @PrimaryKey
  @ColumnInfo(name = "city_id")
  val cityId: Long,

  /**
   * Cloudiness, %
   */
  @ColumnInfo(name = "cloudiness")
  val cloudiness: Long,

  /**
   * Group of weather parameters (Rain, Snow, Extreme etc.)
   */
  @ColumnInfo(name = "main")
  val main: String,

  /**
   * Weather condition within the group
   */
  @ColumnInfo(name = "description")
  val description: String,

  /**
   * Weather icon weatherId
   */
  @ColumnInfo(name = "icon")
  val icon: String,

  /**
   * Temperature. Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
   */
  @ColumnInfo(name = "temperature")
  val temperature: Double,

  /**
   * Atmospheric pressure on the sea level by default, hPa
   */
  @ColumnInfo(name = "pressure")
  val pressure: Double,

  /**
   * Humidity, %
   */
  @ColumnInfo(name = "humidity")
  val humidity: Long,

  /**
   * Minimum temperature at the moment of calculation.
   * This is deviation from 'temp' that is possible for large cities and megalopolises geographically expanded (use these parameter optionally).
   * Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
   */
  @ColumnInfo(name = "temperature_min")
  val temperatureMin: Double,

  /**
   *  Maximum temperature at the moment of calculation.
   *  This is deviation from 'temp' that is possible for large cities and megalopolises geographically expanded (use these parameter optionally).
   *  Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
   */
  @ColumnInfo(name = "temperature_max")
  val temperatureMax: Double,

  /**
   * Wind speed. Unit Default: meter/sec, Metric: meter/sec, Imperial: miles/hour.
   */
  @ColumnInfo(name = "win_speed")
  val winSpeed: Double,

  /**
   *  Wind direction, degrees (meteorological)
   */
  @ColumnInfo(name = "win_degrees")
  val winDegrees: Double,

  /**
   * Time of data forecasted, unix, UTC
   */
  @ColumnInfo(name = "data_time")
  val dataTime: Date,

  /**
   * Rain volume for last 3 hours, mm
   */
  @ColumnInfo(name = "rain_volume_for_last_3_hours")
  val rainVolumeForThe3Hours: Double,

  /**
   * Snow volume for last 3 hours
   */
  @ColumnInfo(name = "snow_volume_for_last_3_hours")
  val snowVolumeForThe3Hours: Double,

  /**
   * Visibility, meter
   */
  @ColumnInfo(name = "visibilityKm")
  val visibility: Double,

  /**
   * Sunrise time, unix, UTC
   */
  @ColumnInfo(name = "sunrise")
  val sunrise: Date,

  /**
   * Sunset time, unix, UTC
   */
  @ColumnInfo(name = "sunset")
  val sunset: Date,

  /**
   * Weather condition weatherId
   */
  @ColumnInfo(name = "weather_condition_id")
  val weatherConditionId: Long
) : Parcelable
