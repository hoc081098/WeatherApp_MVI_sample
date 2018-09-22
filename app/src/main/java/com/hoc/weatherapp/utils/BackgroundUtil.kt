package com.hoc.weatherapp.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.CurrentWeather

/*private val backgrounds = mapOf(
    "01d" to listOf(R.drawable.clear_sky_sun),
    "01n" to listOf(
        R.drawable.sky_full_moon,
        R.drawable.sky_not_full_moon,
        R.drawable.clear_sky_stars
    ),

    "02d" to listOf(R.drawable.clouds_sky_blue),
    "02n" to listOf(
        R.drawable.cloud_night_full_moon,
        R.drawable.cloud_night_not_full_moon,
        R.drawable.cloud_night_sky,
        R.drawable.cloud_night_sky_full_moon
    ),

    "03d" to listOf(R.drawable.scattered_clouds_),
    "03n" to listOf(R.drawable.night_scattered_clouds_),

    "04d" to listOf(R.drawable.clouds_sky_overcast),
    "04n" to listOf(R.drawable.overcast_sky_night),

    "09d" to listOf(R.drawable.rain, R.drawable.rain2),
    "09n" to listOf(R.drawable.rain, R.drawable.rain2),

    "10d" to listOf(R.drawable.rain, R.drawable.rain2),
    "10n" to listOf(R.drawable.rain, R.drawable.rain2),

    "11d" to listOf(R.drawable.thunderstorm, R.drawable.thunderstorm2),
    "11n" to listOf(R.drawable.thunderstorm, R.drawable.thunderstorm3),

    "13d" to listOf(R.drawable.snow),
    "13n" to listOf(R.drawable.snow_night),

    "50d" to listOf(R.drawable.mist, R.drawable.mist2),
    "50n" to listOf(R.drawable.mist_night)
)*/

/**
 * 01d, 01n -> clear sky
 * 02d, 02n -> few clouds
 */

@DrawableRes
fun getBackgroundDrawableFromWeather(weather: CurrentWeather, context: Context): Int {
    if (weather.temperature > 35 + 273.15) { // 35℃
        return R.drawable.bg_hot
    }
    return context.resources.getIdentifier(
        "bg_${weather.icon}",
        "drawable",
        context.packageName
    )
}

@DrawableRes
fun getIconDrawableFromIconString(icon: String): Int {
    return R.mipmap.ic_launcher_round
}