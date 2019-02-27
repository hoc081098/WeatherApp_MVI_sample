package com.hoc.weatherapp.ui.main

import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import com.hoc.weatherapp.R
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class ColorHolderSource {
  private val subject = BehaviorSubject.createDefault(R.color.colorPrimaryDark)

  val vibrantColorObservable: Observable<Int> = subject
  val vibrantColor: Int get() = subject.value!!
  @MainThread
  fun change(@ColorInt color: Int) = subject.onNext(color)
}