package com.hoc.weatherapp.utils

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