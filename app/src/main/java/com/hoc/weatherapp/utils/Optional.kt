package com.hoc.weatherapp.utils

import java.util.*

@Suppress("unused")
sealed class Optional<out T>

data class Some<T : Any>(val value: T) : Optional<T>()
object None : Optional<Nothing>() {
  override fun toString() = "None"
}

fun <T : Any> T?.toOptional(): Optional<T> = when (this) {
  null -> None
  else -> Some(this)
}

fun <T> Optional<T>.getOrNull(): T? = when (this) {
  is Some -> value
  else -> null
}

fun <T> Optional<T>.getOrThrow(): T = when (this) {
  is Some -> value
  else -> throw NoSuchElementException("No value present")
}
