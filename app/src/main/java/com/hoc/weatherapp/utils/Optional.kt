package com.hoc.weatherapp.utils

sealed class Optional<out T>

class Some<T : Any>(val value: T) : Optional<T>()
object None : Optional<Nothing>()

fun <T : Any> T?.toOptional(): Optional<T> = when (this) {
    null -> None
    else -> Some(this)
}