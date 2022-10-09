package com.hoc.weatherapp.data.models

enum class WindDirection {
  N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW;

  companion object {
    fun fromDegrees(deg: Double): WindDirection {
      val d = (deg / 22.5 + 0.5).toInt() % 16
      return WindDirection.values()[d]
    }
  }
}
