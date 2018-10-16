package com.hoc.weatherapp.data.models.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "five_day_forecast", primaryKeys = ["id", "timeOfDataForecasted"])
data class DailyWeather(
    /**
     * City information
     */
    @Embedded val city: City,

    /**
     * Time of data forecasted, unix, UTC
     */
    val timeOfDataForecasted: Date,

    /**
     * Temperature. Unit Default: Kelvin, Metric: Celsius, Imperial: Fahrenheit.
     */
    val temperature: Double,

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
     * Atmospheric pressure on the sea level by default, hPa
     */
    val pressure: Double,

    /**
     * Atmospheric pressure on the sea level, hPa
     */
    val seaLevel: Double,

    /**
     * Atmospheric pressure on the ground level, hPa
     */
    val groundLevel: Double,

    /**
     * Humidity, %
     */
    val humidity: Long,

    /**
     * Group of weather parameters (Rain, Snow, Extreme etc.)
     */
    val main: String,

    /**
     * Weather condition within the group
     */
    val description: String,

    /**
     * Weather icon weatherId
     */
    val icon: String,

    /**
     * Cloudiness, %
     */
    val cloudiness: Long,

    /**
     * Wind speed. Unit Default: meter/sec, Metric: meter/sec, Imperial: miles/hour.
     */
    val winSpeed: Double,

    /**
     *  Wind direction, degrees (meteorological)
     */
    val winDegrees: Double,

    /**
     * Rain volume for last 3 hours, mm
     */
    val rainVolumeForTheLast3Hours: Double,

    /**
     * Snow volume for last 3 hours
     */
    val snowVolumeForTheLast3Hours: Double
) : Parcelable