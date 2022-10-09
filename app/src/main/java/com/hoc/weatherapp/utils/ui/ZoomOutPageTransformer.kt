package com.hoc.weatherapp.utils.ui

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.absoluteValue

class ZoomOutPageTransformer : ViewPager.PageTransformer {
  override fun transformPage(view: View, position: Float) {
    val pageWidth = view.width
    val pageHeight = view.height

    when {
      position < -1 -> // [-Infinity,-1)
        // This page is way off-screen to the left.
        view.alpha = 0f
      position <= 1 -> { // [-1,1]
        // Modify the default slide transition to shrink the page as well
        val scaleFactor = maxOf(MIN_SCALE, 1 - position.absoluteValue)
        val vertMargin = pageHeight * (1 - scaleFactor) / 2
        val horzMargin = pageWidth * (1 - scaleFactor) / 2
        view.run {
          translationX = if (position < 0) {
            horzMargin - vertMargin / 2
          } else {
            -horzMargin + vertMargin / 2
          }

          // Scale the page down (between MIN_SCALE and 1)
          scaleX = scaleFactor
          scaleY = scaleFactor

          // Fade the page relative to its size.
          alpha = MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) *
            (1 - MIN_ALPHA)
        }
      }
      else -> // (1,+Infinity]
        // This page is way off-screen to the right.
        view.alpha = 0f
    }
  }

  companion object {
    private const val MIN_SCALE = 0.85f
    private const val MIN_ALPHA = 0.5f
  }
}
