package com.hoc.weatherapp.data.models.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "current_weathers", primaryKeys = ["id"])
data class CurrentWeather(
    /**
     * City information
     */
    @Embedded val city: City,

    /**
     * Cloudiness, %
     */
    val cloudiness: Long,

    /**
     * Group of weather parameters (Rain, Snow, Extreme etc.)
     */
    val main: String,

    /**
     * Weather condition within the group
     */
    val description: String,

    /**
     * Weather icon id
     */
    val icon: String,

    /**
     * Temperature. Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
     */
    val temperature: Double,

    /**
     * Atmospheric pressure on the sea level by default, hPa
     */
    val pressure: Double,

    /**
     * Humidity, %
     */
    val humidity: Long,

    /**
     * Minimum temperature at the moment of calculation.
     * This is deviation from 'temp' that is possible for large cities and megalopolises geographically expanded (use these parameter optionally).
     * Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
     */
    val temperatureMin: Double,

    /**
     *  Maximum temperature at the moment of calculation.
     *  This is deviation from 'temp' that is possible for large cities and megalopolises geographically expanded (use these parameter optionally).
     *  Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
     */
    val temperatureMax: Double,

    /**
     * Wind speed. Unit Default: meter/sec, Metric: meter/sec, Imperial: miles/hour.
     */
    val winSpeed: Double,

    /**
     *  Wind direction, degrees (meteorological)
     */
    val winDegrees: Double,

    /**
     * Time of data forecasted, unix, UTC
     */
    val dataTime: Date,

    /**
     * Rain volume for last 3 hours, mm
     */
    val rainVolumeForTheLast3Hours: Double,

    /**
     * Snow volume for last 3 hours
     */
    val snowVolumeForTheLast3Hours: Double,

    /**
     * Visibility, meter
     */
    val visibility: Double,

    /**
     * Sunrise time, unix, UTC
     */
    val sunrise: Date,

    /**
     * Sunset time, unix, UTC
     */
    val sunset: Date
) : Parcelable