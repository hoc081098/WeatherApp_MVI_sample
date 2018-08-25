package com.hoc.weatherapp.utils

import androidx.annotation.DrawableRes
import com.hoc.weatherapp.R

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

fun getBackground() {
    TODO()
}