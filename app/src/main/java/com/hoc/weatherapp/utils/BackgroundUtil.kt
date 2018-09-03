package com.hoc.weatherapp.utils

import androidx.annotation.DrawableRes
import com.hoc.weatherapp.R

private val backgrounds = mapOf(
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
)

@DrawableRes
fun getBackgroundDrawableFromIconString(icon: String): Int {
    return backgrounds[icon]!!.random()
}

@DrawableRes
fun getIconDrawableFromIconString(icon: String): Int {
    return when (icon) {
        "01d" -> R.drawable.clear_daytime
        "01n" -> R.drawable.nt_clear_nighttime
        "02d" -> R.drawable.partlycloudy_daytime
        "02n" -> R.drawable.nt_partlycloudy_nighttime
        "03d" -> R.drawable.cloudy_daytime
        "03n" -> R.drawable.nt_cloudy_nighttime
        "04d" -> R.drawable.cloudy_daytime
        "04n" -> R.drawable.nt_cloudy_nighttime
        "09d" -> R.drawable.chancerain_daytime
        "09n" -> R.drawable.nt_chancerain_nighttime
        "10d" -> R.drawable.rain_daytime
        "10n" -> R.drawable.nt_rain_nighttime
        "11d" -> R.drawable.tstorms_daytime
        "11n" -> R.drawable.nt_tstorms_nighttime
        "13d" -> R.drawable.snow_daytime
        "13n" -> R.drawable.nt_snow_nighttime
        "50d" -> R.drawable.fog_daytime
        "50n" -> R.drawable.nt_fog_nighttime
        else -> TODO()
    }
}