package com.hoc.weatherapp.utils

sealed class Optional<out T> {
    companion object
}

class Some<T : Any>(val value: T) : Optional<T>()
object None : Optional<Nothing>()

fun <T : Any> Optional.Companion.ofNullable(value: T?): Optional<T> {
    return if (value === null) None else Some(value)
}