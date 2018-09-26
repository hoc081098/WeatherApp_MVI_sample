package com.hoc.weatherapp.utils

import androidx.annotation.RawRes
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.CurrentWeather

@RawRes
fun getSoundUriFromCurrentWeather(weather: CurrentWeather): Int {
    return when {
        weather.icon in listOf("09d", "09n", "10d", "10n") -> R.raw.rain_day
        weather.icon in listOf("13d", "13n") -> R.raw.snow_day
        weather.icon in listOf("11d", "11n") -> R.raw.thunderstorm_day
        else -> R.raw.clear_day // TODO handle hailstone_day and wind_day
    }
}
