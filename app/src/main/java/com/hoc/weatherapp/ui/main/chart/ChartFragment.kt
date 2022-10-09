package com.hoc.weatherapp.ui.main.chart

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.db.williamchart.view.LineChartView
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.databinding.FragmentChartBinding
import com.hoc.weatherapp.ui.BaseMviFragment
import com.hoc.weatherapp.utils.UnitConverter
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.themeColor
import com.hoc081098.viewbindingdelegate.viewBinding
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale
import org.koin.android.ext.android.get

class ChartFragment :
  BaseMviFragment<ChartContract.View, ChartPresenter>(R.layout.fragment_chart),
  ChartContract.View {
  private val binding by viewBinding<FragmentChartBinding>()

  private val decimalFormat = DecimalFormat("#.#")
  private val labelsFormatter = { number: Float -> decimalFormat.format(number) }

  override fun render(viewState: ChartContract.ViewState) {
    val (weathers, temperatureUnit, pressureUnit, speedUnit) = viewState
    if (weathers.isEmpty()) return

    val gridColor = requireContext().themeColor(R.attr.colorSecondary)

    binding.textTemperature.text =
      getString(R.string.temperature_chart_title, temperatureUnit.symbol())
    drawChart(
      binding.chartTemperature,
      weathers,
      { UnitConverter.convertTemperature(it.temperature, temperatureUnit).toFloat() },
      ContextCompat.getColor(
        requireContext(),
        R.color.colorPrimary
      ),
      gridColor
    )

    binding.textRain.text = getString(R.string.rain_chart_title)
    drawChart(
      binding.chartRain,
      weathers,
      { it.rainVolumeForTheLast3Hours.toFloat() },
      ContextCompat.getColor(
        requireContext(),
        R.color.colorMaterialBlue500
      ),
      gridColor
    )

    binding.textPressure.text = getString(R.string.pressure_chart_title, pressureUnit.symbol())
    drawChart(
      binding.chartPressure,
      weathers,
      { UnitConverter.convertPressure(it.pressure, pressureUnit).toFloat() },
      ContextCompat.getColor(
        requireContext(),
        R.color.colorMaterialCyan400
      ),
      gridColor
    )

    binding.textWindSpeed.text = getString(R.string.wind_speed_chart_title, speedUnit.symbol())
    drawChart(
      binding.chartWindSpeed,
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

  @SuppressLint("Range")
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
