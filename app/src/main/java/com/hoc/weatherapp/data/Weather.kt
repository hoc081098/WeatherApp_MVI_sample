package com.hoc.weatherapp.data

import androidx.room.Embedded
import androidx.room.Entity
import java.util.*

@Entity(tableName = "weathers", primaryKeys = ["id"])
data class Weather(
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
        val rainVolumeForTheLast3Hours: Long,
        val snowVolumeForTheLast3Hours: Long
)