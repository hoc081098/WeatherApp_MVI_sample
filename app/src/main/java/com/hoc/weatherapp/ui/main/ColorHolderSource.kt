package com.hoc.weatherapp.ui.main

import android.app.Application
import androidx.annotation.MainThread
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.asObservable
import com.hoc.weatherapp.utils.themeColor
import io.reactivex.subjects.BehaviorSubject

class ColorHolderSource(androidApplication: Application) {
  val defaultColorPrimaryVariant = androidApplication.themeColor(R.attr.colorPrimaryVariant)
  val defaultColorSecondary = androidApplication.themeColor(R.attr.colorSecondary)

  private val subject = BehaviorSubject.createDefault(
    defaultColorPrimaryVariant to defaultColorSecondary
  )

  val colorObservable = subject.asObservable()

  @MainThread
  fun change(colors: Pair<Int, Int>) = subject.onNext(colors)
}
