package com.hoc.weatherapp.utils

import java.io.Serializable

private object UninitializedValue

private class InitializedLazyImpl<out T>(override val value: T) : Lazy<T>, Serializable {
  override fun isInitialized(): Boolean = true
  override fun toString(): String = value.toString()
}

internal class MyUnsafeLazyImpl<out T>(initializer: () -> T) : Lazy<T>, Serializable {
  private var initializer: (() -> T)? = initializer
  private var _value: Any? = UninitializedValue

  override val value: T
    get() {
      if (_value === UninitializedValue) {
        _value = initializer!!()
        initializer = null
      }
      @Suppress("UNCHECKED_CAST")
      return _value as T
    }

  override fun isInitialized(): Boolean = _value !== UninitializedValue

  override fun toString(): String =
    if (isInitialized()) value.toString() else "Lazy value not initialized yet."

  private fun writeReplace(): Any = InitializedLazyImpl(value)

  fun cleanUp() {
    initializer = null
  }
}
