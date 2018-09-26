package com.hoc.weatherapp.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.data.models.entity.DailyWeather
import java.util.Calendar

/**
 * 01d, 01n -> clear sky
 * 02d, 02n -> few clouds
 * 03d, 03n -> scattered clouds
 * 04d, 04n -> broken clouds
 * 09d, 09n -> shower rain
 * 10d, 10n -> rain
 * 11d, 11n -> thunderstorm
 * 13d, 13n -> snow
 * 50d, 50n -> mist
 */

private fun isDay(w: CurrentWeather): Boolean {
    val calendar = Calendar.getInstance()
    w.debug("${calendar.time} ${w.sunrise} ${w.sunset}")
    return (calendar.time in w.sunrise..w.sunset)
        .also { it.debug("isDay = $it", "@@@") }
}

@DrawableRes
fun getBackgroundDrawableFromWeather(weather: CurrentWeather): Int {
    return when {
        weather.weatherConditionId == 800L
            && isDay(weather)
            && weather.temperature > 35 + 273.15 /* 35℃ */ -> {
            R.drawable.hot_bg
        }

        weather.icon == "01d" -> {
            R.drawable.clear_day_bg
        }

        weather.icon == "01n" -> {
            R.drawable.clear_night_bg
        }

        weather.weatherConditionId / 100 == 8L -> {
            if (isDay(weather)) {
                R.drawable.cloud_day_bg
            } else {
                R.drawable.cloud_night_bg
            }
        }

        weather.weatherConditionId / 100 in listOf(5L, 2L) -> {
            if (isDay(weather)) {
                R.drawable.rain_day_bg
            } else {
                R.drawable.rain_night_bg
            }
        }

        else -> R.drawable.default_bg
    }
}

@DrawableRes
fun Context.getIconDrawableFromCurrentWeather(weather: CurrentWeather): Int {
    if (weather.weatherConditionId == 741L) {
        return R.drawable.weather_foggy
    }
    if (weather.weatherConditionId == 761L) {
        return R.drawable.weather_dust
    }
    if (weather.weatherConditionId == 721L) {
        return R.drawable.weather_haze
    }
    if (weather.weatherConditionId == 781L) {
        return R.drawable.weather_tornado
    }
    return resources.getIdentifier(
        "weather_icon_${weather.icon}",
        "drawable",
        packageName
    ).takeIf { it != 0 } ?: R.drawable.weather_icon_null
}

@DrawableRes
fun Context.getIconDrawableFromDailyWeather(weather: DailyWeather): Int {
    return resources.getIdentifier(
        "weather_icon_${weather.icon}",
        "drawable",
        packageName
    ).takeIf { it != 0 } ?: R.drawable.weather_icon_null
}