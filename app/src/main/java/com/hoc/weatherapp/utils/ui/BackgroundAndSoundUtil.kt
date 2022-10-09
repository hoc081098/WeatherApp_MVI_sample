package com.hoc.weatherapp.utils.ui

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.utils.toZonedDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

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

private fun isDay(
  w: CurrentWeather,
  city: City
): Boolean {
  val now = ZonedDateTime.now(ZoneId.of(city.zoneId))
  return now in w.sunrise.toZonedDateTime(city.zoneId)..w.sunset.toZonedDateTime(city.zoneId)
}

@DrawableRes
fun getBackgroundDrawableFromWeather(weather: CurrentWeather, city: City): Int {
  return when {
    weather.weatherConditionId == 800L &&
      isDay(weather, city) &&
      weather.temperature > 35 + 273.15 /* 35℃ */ -> {
      R.drawable.hot_bg
    }

    weather.icon == "01d" -> {
      R.drawable.clear_day_bg
    }

    weather.icon == "01n" -> {
      R.drawable.clear_night_bg
    }

    weather.weatherConditionId / 100 == 8L -> {
      if (isDay(weather, city)) {
        R.drawable.cloud_day_bg
      } else {
        R.drawable.cloud_night_bg
      }
    }

    weather.weatherConditionId / 100 in listOf(5L, 2L) -> {
      if (isDay(weather, city)) {
        R.drawable.rain_day_bg
      } else {
        R.drawable.rain_night_bg
      }
    }

    else -> R.drawable.default_bg
  }
}

@DrawableRes
fun Context.getIconDrawableFromCurrentWeather(
  weatherConditionId: Long,
  weatherIcon: String
): Int {
  if (weatherConditionId == 741L) {
    return R.drawable.weather_foggy
  }
  if (weatherConditionId == 761L) {
    return R.drawable.weather_dust
  }
  if (weatherConditionId == 721L) {
    return R.drawable.weather_haze
  }
  if (weatherConditionId == 781L) {
    return R.drawable.weather_tornado
  }
  return resources.getIdentifier(
    "weather_icon_$weatherIcon",
    "drawable",
    packageName
  ).takeIf { it != 0 } ?: R.drawable.weather_icon_null
}

@DrawableRes
fun Context.getIconDrawableFromDailyWeather(icon: String): Int {
  return resources.getIdentifier(
    "weather_icon_$icon",
    "drawable",
    packageName
  ).takeIf { it != 0 } ?: R.drawable.weather_icon_null
}

@RawRes
fun getSoundUriFromCurrentWeather(weather: CurrentWeather): Int {
  return when (weather.icon) {
    in listOf("09d", "09n", "10d", "10n") -> R.raw.rain_day
    in listOf("13d", "13n") -> R.raw.snow_day
    in listOf("11d", "11n") -> R.raw.thunderstorm_day
    else -> R.raw.clear_day // TODO handle hailstone_day and wind_day
  }
}
