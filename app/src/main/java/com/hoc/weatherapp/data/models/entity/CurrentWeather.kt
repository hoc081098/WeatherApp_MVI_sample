package com.hoc.weatherapp.data.models.entity

import androidx.room.Embedded
import androidx.room.Entity
import java.util.Date

@Entity(tableName = "current_weathers", primaryKeys = ["id"])
data class CurrentWeather(
        @Embedded val city: City,
        val cloudiness: Long,
        val main: String,
        val description: String,
        val icon: String,
        val temperature: Double,
        val pressure: Double,
        val humidity: Long,
        val temperatureMin: Double,
        val temperatureMax: Double,
        val winSpeed: Double,
        val winDegrees: Double,
        val dataTime: Date,
    val rainVolumeForTheLast3Hours: Double,
    val snowVolumeForTheLast3Hours: Double
)