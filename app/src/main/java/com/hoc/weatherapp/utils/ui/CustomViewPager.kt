package com.hoc.weatherapp.utils.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewPager @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
  var pagingEnable = false

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(ev: MotionEvent?): Boolean {
    return pagingEnable && super.onTouchEvent(ev)
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return pagingEnable && super.onInterceptTouchEvent(ev)
  }
}
