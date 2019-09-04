package com.hoc.weatherapp.ui.main

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.asObservable
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.themeColor
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject

@SuppressLint("CheckResult")
class ColorHolderSource(androidApplication: Application) {
  private val subject = BehaviorSubject.createDefault(
    androidApplication.themeColor(R.attr.colorPrimaryDark) to
        androidApplication.themeColor(R.attr.colorAccent)
  )

  val colorObservable = subject.asObservable()

  @MainThread fun change(colors: Pair<Int, Int>) = subject.onNext(colors)
}