package com.hoc.weatherapp.utils

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*

private val CALENDAR = Calendar.getInstance()

/**
 * Return Date with hour, minute, second and millisecond set to ZERO
 */
fun Date.trim(): Date {
  CALENDAR.time = this
  CALENDAR[Calendar.HOUR_OF_DAY] = 0
  CALENDAR[Calendar.MINUTE] = 0
  CALENDAR[Calendar.SECOND] = 0
  CALENDAR[Calendar.MILLISECOND] = 0
  return CALENDAR.time
}

fun Date.toZonedDateTime(zoneId: String): ZonedDateTime {
  return Instant.ofEpochMilli(time)
      .atZone(runCatching { ZoneId.of(zoneId) }.getOrElse { ZoneId.systemDefault() })
}
