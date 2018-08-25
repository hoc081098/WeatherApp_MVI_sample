package com.hoc.weatherapp.data

import com.squareup.moshi.Json

data class NearCity(
        val id: Long,
        val name: String,
        val country: String,
        val coord: Coord
)

class Clouds(
        @Json(name = "all")
        val all: Long? = null
)

class Coord(
        @Json(name = "lon")
        val lon: Double? = null,
        @Json(name = "lat")
        val lat: Double? = null
)

class Main(
        @Json(name = "temp")
        val temp: Double? = null,
        @Json(name = "pressure")
        val pressure: Double? = null,
        @Json(name = "humidity")
        val humidity: Long? = null,
        @Json(name = "temp_min")
        val tempMin: Double? = null,
        @Json(name = "temp_max")
        val tempMax: Double? = null
)

class Sys(
        @Json(name = "type")
        val type: Long? = null,
        @Json(name = "id")
        val id: Long? = null,
        @Json(name = "message")
        val message: Double? = null,
        @Json(name = "country")
        val country: String? = null,
        @Json(name = "sunrise")
        val sunrise: Long? = null,
        @Json(name = "sunset")
        val sunset: Long? = null
)

class WeatherModel(
        @Json(name = "id")
        val id: Long? = null,
        @Json(name = "main")
        val main: String? = null,
        @Json(name = "description")
        val description: String? = null,
        @Json(name = "icon")
        val icon: String? = null
)

class WeatherResponse(
        @Json(name = "coord")
        val coord: Coord? = null,
        @Json(name = "weather")
        val weather: List<WeatherModel>? = null,
        @Json(name = "base")
        val base: String? = null,
        @Json(name = "main")
        val main: Main? = null,
        @Json(name = "visibility")
        val visibility: Long? = null,
        @Json(name = "wind")
        val wind: Wind? = null,
        @Json(name = "clouds")
        val clouds: Clouds? = null,
        @Json(name = "dt")
        val dt: Long? = null,
        @Json(name = "sys")
        val sys: Sys? = null,
        @Json(name = "id")
        val id: Long? = null,
        @Json(name = "name")
        val name: String? = null,
        @Json(name = "cod")
        val cod: Long? = null,
        @Json(name = "rain")
        val rain: Rain? = null,
        @Json(name = "snow")
        val snow: Snow? = null
)

class Snow(
        @Json(name = "3h")
        val _3h: Long? = null
)

class Rain(
        @Json(name = "3h")
        val _3h: Long? = null
)

class Wind(
        @Json(name = "speed")
        val speed: Double? = null,
        @Json(name = "deg")
        val deg: Double? = null
)
