package com.hoc.weatherapp.ui.main

import android.annotation.SuppressLint
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.asObservable
import com.hoc.weatherapp.utils.debug
import io.reactivex.subjects.BehaviorSubject

@SuppressLint("CheckResult")
class ColorHolderSource {
  private val subject = BehaviorSubject.createDefault(R.color.colorPrimaryDark)

  val vibrantColorObservable = subject.asObservable()

  init {
    vibrantColorObservable.subscribe {
      debug("Color=$it, ${R.color.colorPrimaryDark}", "ColorHolderSource")
    }
  }


  val vibrantColor get() = subject.value!!

  @MainThread
  fun change(@ColorInt color: Int) = subject.onNext(color)
}