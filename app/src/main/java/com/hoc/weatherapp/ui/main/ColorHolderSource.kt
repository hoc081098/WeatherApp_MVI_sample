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
class ColorHolderSource(private val androidApplication: Application) {
  private val subject = BehaviorSubject.create<@ColorInt Int>()

  val vibrantColorObservable = subject.asObservable()

  val vibrantColor: Int
    @ColorInt get() = subject.value ?: ContextCompat.getColor(
      androidApplication,
      R.color.colorPrimaryDark
    )

  init {
    debug("ColorHolderSource::init", "ColorHolderSource")
    vibrantColorObservable.subscribeBy {
      debug(
        "ColorHolderSource onNext=$it",
        "ColorHolderSource"
      )
    }
  }

  @MainThread
  fun change(@ColorInt color: Int) = subject.onNext(color)
}