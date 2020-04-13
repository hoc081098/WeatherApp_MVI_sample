package com.hoc.weatherapp.ui.main.chart

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.db.chart.model.LineSet
import com.db.chart.util.Tools
import com.db.chart.view.LineChartView
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.UnitConverter
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.themeColor
import kotlinx.android.synthetic.main.fragment_chart.*
import org.koin.android.ext.android.get
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

class ChartFragment : MviFragment<ChartContract.View, ChartPresenter>(), ChartContract.View {
  override fun render(viewState: ChartContract.ViewState) {
    val (weathers, temperatureUnit, pressureUnit, speedUnit) = viewState
    if (weathers.isEmpty()) return

    val gridColor = requireContext().themeColor(R.attr.colorAccent)

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

  private inline fun drawChart(
      lineChartView: LineChartView,
      dailyWeathers: List<DailyWeather>,
      crossinline transform: (DailyWeather) -> Float,
      @ColorInt lineColor: Int,
      @ColorInt gridColor: Int
  ) {
    debug("::drawChart")
    lineChartView.run {
      reset()

      val map = dailyWeathers.map(transform)

      LineSet()
          .apply {
            map.zip(getLabels(dailyWeathers))
                .forEach { (value, label) ->
                  addPoint(label, value)
                }
          }
          .setSmooth(true)
          .setColor(lineColor)
          .setThickness(4f)
          .let(::addData)

      // Grid
      val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = gridColor
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        strokeWidth = 1f
      }

      val min = floor(map.min()!!) - 1
      val max = ceil(map.max()!!)

      setGrid(ceil(max - min).toInt(), 0, paint)
          .setBorderSpacing(Tools.fromDpToPx(8f).toInt())
          .setAxisBorderValues(min, max)
          .setStep(2)
          .setXAxis(false)
          .setYAxis(false)
          .setAxisColor(requireContext().themeColor(R.attr.colorOnSurface))
          .setLabelsColor(requireContext().themeColor(R.attr.colorOnSurface))
          .show()
    }
  }

  private fun getLabels(dailyWeathers: List<DailyWeather>): Iterable<String> {
    val instance = Calendar.getInstance()
    var previousDate = 0

    return dailyWeathers.map {
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
}