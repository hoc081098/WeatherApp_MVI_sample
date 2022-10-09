package com.hoc.weatherapp.utils.ui

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import com.hoc.weatherapp.R
import com.hoc.weatherapp.databinding.WindmillLayoutBinding
import com.hoc081098.viewbindingdelegate.inflateViewBinding

class WindmillView @JvmOverloads constructor(
  context: Context?,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {
  private val binding = inflateViewBinding<WindmillLayoutBinding>(true)

  var winSpeed: Double = 0.0
    set(value) {
      val anim = AnimationUtils.loadAnimation(context, R.anim.windmill)
        .apply {
          interpolator = LinearInterpolator()
          duration = calculateDuration(value)
        }
      binding.blade.startAnimation(anim)
    }

  private fun calculateDuration(value: Double): Long {
    return (maxOf((1 - value / MAX_WIND_SPEED), 0.0) * MAX_ANIM_DURATION).toLong()
  }

  init {
    val anim = AnimationUtils.loadAnimation(context, R.anim.windmill)
      .apply {
        interpolator = LinearInterpolator()
        duration = calculateDuration(winSpeed)
      }
    binding.blade.startAnimation(anim)
  }

  companion object {
    private const val MAX_ANIM_DURATION = 10_000
    private const val MAX_WIND_SPEED = 20.0
  }
}
