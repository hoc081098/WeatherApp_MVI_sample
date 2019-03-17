package com.hoc.weatherapp.ui.main

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.asObservable
import com.hoc.weatherapp.utils.debug
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject

@SuppressLint("CheckResult")
class ColorHolderSource(androidApplication: Application) {
  private val subject = BehaviorSubject.createDefault<@ColorInt Int>(
    ContextCompat.getColor(
      androidApplication,
      R.color.colorPrimaryDark
    )
  )

  val colorObservable = subject.asObservable()

  init {
    debug("ColorHolderSource::init", "ColorHolderSource")

    colorObservable.subscribeBy {
      debug(
        "ColorHolderSource onNext=$it",
        "ColorHolderSource"
      )
    }
  }

  @MainThread fun change(@ColorInt color: Int) = subject.onNext(color)
}