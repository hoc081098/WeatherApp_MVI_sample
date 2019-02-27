package com.hoc.weatherapp.utils

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*

val calendar = Calendar.getInstance()!!

/**
 * Return Date with hour, minute, second and millisecond set to ZERO
 */
fun Date.trim(): Date {
  calendar.time = this
  calendar[Calendar.HOUR_OF_DAY] = 0
  calendar[Calendar.MINUTE] = 0
  calendar[Calendar.SECOND] = 0
  calendar[Calendar.MILLISECOND] = 0
  return calendar.time
}

fun Date.toZonedDateTime(zoneId: String): ZonedDateTime {
  return Instant.ofEpochMilli(time)
    .atZone(runCatching { ZoneId.of(zoneId) }.getOrElse { ZoneId.systemDefault() })
}
