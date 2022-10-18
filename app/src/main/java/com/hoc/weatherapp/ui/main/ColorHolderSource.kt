package com.hoc.weatherapp.ui.main

import android.app.Application
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.themeColor
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.parcelize.Parcelize

class ColorHolderSource(androidApplication: Application) {
  @Parcelize
  data class Colors(
    @ColorInt
    val statusBarColor: Int,
    @ColorInt
    val backgroundColor: Int // TODO: rename
  ) : Parcelable

  @ColorInt
  val defaultStatusBarColor = androidApplication.themeColor(R.attr.colorPrimaryVariant)

  @ColorInt
  val defaultBackgroundColor = androidApplication.themeColor(R.attr.colorSecondary)

  private val subject = BehaviorSubject.createDefault(
    Colors(
      defaultStatusBarColor,
      defaultBackgroundColor
    )
  )

  val colorObservable: Observable<Colors> = subject.hide()

  @MainThread
  fun change(colors: Colors) = subject.onNext(colors)
}
