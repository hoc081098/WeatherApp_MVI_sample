package com.hoc.weatherapp.ui.main.fivedayforecast

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hoc.weatherapp.R
import com.hoc.weatherapp.databinding.ActivityDetailDailyWeatherBinding
import com.hoc.weatherapp.databinding.DetailItemLayoutBinding
import com.hoc.weatherapp.ui.BaseAppCompatActivity
import com.hoc.weatherapp.utils.Mode
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.setColorFilter
import com.hoc.weatherapp.utils.ui.getIconDrawableFromDailyWeather
import com.hoc081098.viewbindingdelegate.inflateViewBinding
import com.hoc081098.viewbindingdelegate.viewBinding
import org.threeten.bp.format.DateTimeFormatter

@ExperimentalStdlibApi
class DailyDetailActivity : BaseAppCompatActivity(R.layout.activity_detail_daily_weather) {
  companion object {
    const val TAG = "com.hoc.weatherapp.ui.main.fivedayforecast.daily_detail_activity"
    const val ITEM_KEY =
      "com.hoc.weatherapp.ui.main.fivedayforecast.daily_detail_activity_item"
  }

  private val binding by viewBinding<ActivityDetailDailyWeatherBinding>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

    bind(intent.getParcelableExtra(ITEM_KEY)!!)
  }

  private fun bind(item: DailyWeatherListItem.Weather) = binding.run {
    item.colors.second.let { color ->
      window.statusBarColor = color
      textDataTime.setTextColor(color)
      ContextCompat.getDrawable(
        this@DailyDetailActivity,
        R.drawable.ic_navigate_before_black_24dp
      )!!.mutate()
        .apply { setColorFilter(color, Mode.SRC_IN) }
        .let { imageBack.setImageDrawable(it) }
    }
    imageBack.setOnClickListener { finish() }

    imageIcon.setImageResource(getIconDrawableFromDailyWeather(item.weatherIcon))
    imageIcon.setBackgroundColor(item.colors.second)
    textDataTime.text = item.dataTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"))
    textMain.text = item.main
    textDescription.text = item.weatherDescription

    recyclerDetail.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = object : RecyclerView.Adapter<VH>() {
        val list = listOf(
          R.drawable.ic_thermometer_black_24dp to "Temperature min: ${item.temperatureMin}",
          R.drawable.ic_thermometer_black_24dp to "Temperature max: ${item.temperatureMax}",
          R.drawable.ic_thermometer_black_24dp to "Temperature: ${item.temperature}",
          R.drawable.ic_pressure_black_24dp to "Pressure: ${item.pressure}",
          R.drawable.ic_pressure_black_24dp to "Sea level: ${item.seaLevel}",
          R.drawable.ic_pressure_black_24dp to "Ground level: ${item.groundLevel}",
          R.drawable.ic_humidity_black_24dp to "Humidity: ${item.humidity}",
          R.drawable.ic_cloud_black_24dp to "Cloudiness: ${item.cloudiness}",
          R.drawable.ic_windy_black_24dp to "Wind speed: ${item.winSpeed}",
          R.drawable.ic_windy_black_24dp to "Wind direction: ${item.windDirection}",
          R.drawable.ic_water_black_24dp to "Rain volume last 3h: ${item.rainVolumeForTheLast3Hours}",
          R.drawable.ic_snow_black_24dp to "Snow volume last 3h: ${item.snowVolumeForTheLast3Hours}"
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
          VH(parent inflateViewBinding false)

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: VH, position: Int) =
          holder.bind(list[position], item.colors.second).also {
            debug("Bind $position ${list[position]}", "######")
          }
      }

      addItemDecoration(
        DividerItemDecoration(
          context,
          (layoutManager as LinearLayoutManager).orientation
        )
      )
    }
  }

  override fun onOptionsItemSelected(item: MenuItem) =
    if (item.itemId == android.R.id.home) {
      true.also { finish() }
    } else {
      super.onOptionsItemSelected(item)
    }

  class VH(private val binding: DetailItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(pair: Pair<Int, String>, @ColorInt iconBackgroundColor: Int) = binding.run {
      ContextCompat.getDrawable(itemView.context, pair.first)!!.mutate()
        .apply { setColorFilter(iconBackgroundColor, Mode.SRC_IN) }
        .let { imageView5.setImageDrawable(it) }

      textView.text = pair.second
    }
  }
}
