package com.hoc.weatherapp.data.models.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
  tableName = "five_day_forecast",
  foreignKeys = [
    ForeignKey(
      entity = City::class,
      onDelete = CASCADE,
      onUpdate = CASCADE,
      parentColumns = ["id"],
      childColumns = ["city_id"]
    )
  ],
  indices = [Index(value = ["city_id"])]
)
data class DailyWeather(
  /**
   * Time of data forecasted, unix, UTC
   */
  @ColumnInfo(name = "data_time")
  val timeOfDataForecasted: Date,

  /**
   * Temperature. Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
   */
  @ColumnInfo(name = "temperature")
  val temperature: Double,

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
   * Atmospheric pressure on the sea level by default, hPa
   */
  @ColumnInfo(name = "pressure")
  val pressure: Double,

  /**
   * Atmospheric pressure on the sea level, hPa
   */
  @ColumnInfo(name = "sea_level")
  val seaLevel: Double,

  /**
   * Atmospheric pressure on the ground level, hPa
   */
  @ColumnInfo(name = "ground_level")
  val groundLevel: Double,

  /**
   * Humidity, %
   */
  @ColumnInfo(name = "humidity")
  val humidity: Long,

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
   * Cloudiness, %
   */
  @ColumnInfo(name = "cloudiness")
  val cloudiness: Long,

  /**
   * Wind speed. Unit Default: meter/sec, Metric: meter/sec, Imperial: miles/hour.
   */
  @ColumnInfo(name = "win_speed")
  val windSpeed: Double,

  /**
   *  Wind direction, degrees (meteorological)
   */
  @ColumnInfo(name = "win_degrees")
  val winDegrees: Double,

  /**
   * Rain volume for last 3 hours, mm
   */
  @ColumnInfo(name = "rain_volume_for_last_3_hours")
  val rainVolumeForTheLast3Hours: Double,

  /**
   * Snow volume for last 3 hours
   */
  @ColumnInfo(name = "snow_volume_for_last_3_hours")
  val snowVolumeForTheLast3Hours: Double,

  /**
   * Id of city
   */
  @ColumnInfo(name = "city_id")
  val cityId: Long
) : Parcelable {
  @IgnoredOnParcel
  @ColumnInfo(name = "id")
  @PrimaryKey(autoGenerate = true)
  var id: Long = 0
}
