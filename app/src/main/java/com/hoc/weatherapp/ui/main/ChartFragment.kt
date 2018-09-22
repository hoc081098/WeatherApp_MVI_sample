package com.hoc.weatherapp.ui.main

import android.content.IntentFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.db.chart.model.LineSet
import com.db.chart.util.Tools
import com.db.chart.view.LineChartView
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.local.DailyWeatherDao
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.data.remote.TemperatureUnit
import com.hoc.weatherapp.ui.SettingsActivity.SettingFragment.Companion.ACTION_CHANGED_TEMPERATURE_UNIT
import com.hoc.weatherapp.ui.SettingsActivity.SettingFragment.Companion.EXTRA_TEMPERATURE_UNIT
import com.hoc.weatherapp.utils.SharedPrefUtil
import com.hoc.weatherapp.utils.UnitConvertor
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.rxIntentFlowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_chart.*
import org.koin.android.ext.android.inject
import java.util.Calendar
import java.util.Locale

class ChartFragment : Fragment() {
    private val dailyWeatherDao by inject<DailyWeatherDao>()
    private val sharedPrefUtil by inject<SharedPrefUtil>()

    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chart, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val temperatureUnitFlowable = rxIntentFlowable(
            IntentFilter().apply { addAction(ACTION_CHANGED_TEMPERATURE_UNIT) },
            requireContext()
        )
            .filter { it.action == ACTION_CHANGED_TEMPERATURE_UNIT }
            .map { it.getStringExtra(EXTRA_TEMPERATURE_UNIT)!! }
            .map { TemperatureUnit.fromString(it) }
            .startWith(sharedPrefUtil.temperatureUnit)

        sharedPrefUtil.selectedCity?.let { city ->
            dailyWeatherDao.getAllDailyWeathersByCityId(city.id)
                .combineLatest(temperatureUnitFlowable)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { debug("${it.first.size}, ${it.second.symbol()}", "@@@") }
                .subscribeBy(
                    onError = { throw it },
                    onNext = ::updateCharts
                )
                .addTo(compositeDisposable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debug("ChartFragment::onDestroyView", "@@@")
        compositeDisposable.clear()
    }

    private fun updateCharts(pair: Pair<List<DailyWeather>, TemperatureUnit>) {
        val (dailyWeathers, temperatureUnit) = pair
        text_temperature.text = "Temperature (${temperatureUnit.symbol()})"
        drawChart(
            chart_temperature,
            dailyWeathers,
            { UnitConvertor.convertTemperature(it.temperature, temperatureUnit).toFloat() },
            ContextCompat.getColor(
                requireContext(),
                R.color.colorPrimary
            ),
            ContextCompat.getColor(
                requireContext(),
                R.color.colorAccent
            )
        )

        text_rain.text = "Rain volume (mm)"
        drawChart(
            chart_rain,
            dailyWeathers,
            { it.rainVolumeForTheLast3Hours.toFloat() },
            ContextCompat.getColor(
                requireContext(),
                R.color.colorMaterialBlue500
            ),
            ContextCompat.getColor(
                requireContext(),
                R.color.colorAccent
            )
        )

        text_pressure.text = "Pressure (hPa)"
        drawChart(
            chart_pressure,
            dailyWeathers,
            { it.pressure.toFloat() },
            ContextCompat.getColor(
                requireContext(),
                R.color.colorDeepPurpleAccent700
            ),
            ContextCompat.getColor(
                requireContext(),
                R.color.colorAccent
            )
        )
    }

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

            val min = map.min()!!
            val max = map.max()!!

            setGrid((max - min + 1).toInt(), 0, paint)
                .setBorderSpacing(Tools.fromDpToPx(8f).toInt())
                .setAxisBorderValues(min, max)
                .setStep(2)
                .setXAxis(false)
                .setYAxis(false)
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