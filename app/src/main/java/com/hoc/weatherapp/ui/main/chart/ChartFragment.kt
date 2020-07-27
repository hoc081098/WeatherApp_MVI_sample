package com.hoc.weatherapp.ui.main.chart

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.db.williamchart.ExperimentalFeature
import com.db.williamchart.slidertooltip.SliderTooltip
import com.db.williamchart.view.LineChartView
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.UnitConverter
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.themeColor
import kotlinx.android.synthetic.main.fragment_chart.*
import org.koin.android.ext.android.get
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.ceil
import kotlin.math.floor

class ChartFragment : MviFragment<ChartContract.View, ChartPresenter>(), ChartContract.View {
  private val decimalFormat = DecimalFormat("#.#")
  private val labelsFormatter = { number: Float -> decimalFormat.format(number) }

  override fun render(viewState: ChartContract.ViewState) {
    val (weathers, temperatureUnit, pressureUnit, speedUnit) = viewState
    if (weathers.isEmpty()) return

    val gridColor = requireContext().themeColor(R.attr.colorSecondary)

    text_temperature.text =
        getString(R.string.temperature_chart_title, temperatureUnit.symbol())
    drawChart(
        chart_temperature,
        weathers,
        { UnitConverter.convertTemperature(it.temperature, temperatureUnit).toFloat() },
        ContextCompat.getColor(
            requireContext(),
            R.color.colorPrimary
        ),
        gridColor
    )

    text_rain.text = getString(R.string.rain_chart_title)
    drawChart(
        chart_rain,
        weathers,
        { it.rainVolumeForTheLast3Hours.toFloat() },
        ContextCompat.getColor(
            requireContext(),
            R.color.colorMaterialBlue500
        ),
        gridColor
    )

    text_pressure.text = getString(R.string.pressure_chart_title, pressureUnit.symbol())
    drawChart(
        chart_pressure,
        weathers,
        { UnitConverter.convertPressure(it.pressure, pressureUnit).toFloat() },
        ContextCompat.getColor(
            requireContext(),
            R.color.colorMaterialCyan400
        ),
        gridColor
    )


    text_wind_speed.text = getString(R.string.wind_speed_chart_title, speedUnit.symbol())
    drawChart(
        chart_wind_speed,
        weathers,
        { UnitConverter.convertSpeed(it.windSpeed, speedUnit).toFloat() },
        ContextCompat.getColor(
            requireContext(),
            R.color.colorDeepPurpleAccent700
        ),
        gridColor
    )
  }

  override fun createPresenter() = get<ChartPresenter>()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_chart, container, false)

  @SuppressLint("Range")
  @OptIn(ExperimentalFeature::class)
  private inline fun drawChart(
      lineChartView: LineChartView,
      dailyWeathers: List<DailyWeather>,
      crossinline transform: (DailyWeather) -> Float,
      @ColorInt lineColor: Int,
      @ColorInt gridColor: Int
  ) {
    debug("::drawChart")

    lineChartView.run {
      val map = dailyWeathers.map(transform)

      labelsFormatter = this@ChartFragment.labelsFormatter
      smooth = true
      lineThickness = 4f
      this.lineColor = lineColor
      gradientFillColors = intArrayOf(
          ColorUtils.setAlphaComponent(lineColor, 0x81),
          Color.TRANSPARENT,
      )
      animation.duration = animationDuration
      animate(
          LinkedHashMap(
              dailyWeathers.getLabels()
                  .zip(map)
                  .toMap()
          )
      )

      // Grid
//      val paint = Paint().apply {
//        style = Paint.Style.STROKE
//        isAntiAlias = true
//        color = gridColor
//        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
//        strokeWidth = 1f
//      }
//
//      val min = floor(map.min()!!) - 1
//      val max = ceil(map.max()!!)
//
//      setGrid(ceil(max - min).toInt(), 0, paint)
//          .setBorderSpacing(Tools.fromDpToPx(8f).toInt())
//          .setAxisBorderValues(min, max)
//          .setStep(2)
//          .setXAxis(false)
//          .setYAxis(false)
//          .setAxisColor(requireContext().themeColor(R.attr.colorOnSurface))
//          .setLabelsColor(requireContext().themeColor(R.attr.colorOnSurface))
//          .show()
    }
  }

  private fun List<DailyWeather>.getLabels(): List<String> {
    val instance = Calendar.getInstance()
    var previousDate = 0

    return map {
      instance.time = it.timeOfDataForecasted
      val date = instance[Calendar.DATE]

      when {
        date != previousDate -> {
          previousDate = date
          instance.getDisplayName(
              Calendar.DAY_OF_WEEK,
              Calendar.SHORT,
              Locale.getDefault()
          )
        }
        else -> " "
      }
    }
  }

  private companion object {
    const val animationDuration = 1000L
  }
}