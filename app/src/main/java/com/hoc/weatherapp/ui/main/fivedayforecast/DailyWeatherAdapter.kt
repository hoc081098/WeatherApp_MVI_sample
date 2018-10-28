package com.hoc.weatherapp.ui.main.fivedayforecast

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.getIconDrawableFromDailyWeather
import com.hoc.weatherapp.utils.trim
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.android.synthetic.main.daily_weather_header_layout.view.*
import kotlinx.android.synthetic.main.daily_weather_item_layout.view.*

class DailyWeatherAdapter :
  ListAdapter<DailyWeatherListItem, RecyclerView.ViewHolder>(object :
    DiffUtil.ItemCallback<DailyWeatherListItem?>() {
    override fun areItemsTheSame(
      oldItem: DailyWeatherListItem,
      newItem: DailyWeatherListItem
    ) = when {
      oldItem is DailyWeatherListItem.Weather && newItem is DailyWeatherListItem.Weather -> oldItem.dataTime == newItem.dataTime
      oldItem is DailyWeatherListItem.Header && newItem is DailyWeatherListItem.Header -> oldItem.date == newItem.date
      else -> oldItem == newItem
    }

    override fun areContentsTheSame(
      oldItem: DailyWeatherListItem,
      newItem: DailyWeatherListItem
    ) = newItem == oldItem
  }) {
  @ViewType
  override fun getItemViewType(position: Int): Int {
    return when (
      val item = getItem(position)) {
      is DailyWeatherListItem.Header -> HEADER_TYPE
      is DailyWeatherListItem.Weather -> DAILY_WEATHER_TYPE
      else -> throw  IllegalStateException("Unknown type of item $item at position $position")
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, @ViewType viewType: Int): RecyclerView.ViewHolder {
    return when (viewType) {
      HEADER_TYPE -> LayoutInflater.from(parent.context).inflate(
        R.layout.daily_weather_header_layout,
        parent,
        false
      ).let(::HeaderViewHolder)
      DAILY_WEATHER_TYPE -> LayoutInflater.from(parent.context).inflate(
        R.layout.daily_weather_item_layout,
        parent,
        false
      ).let(::DailyWeatherViewHolder)
      else -> throw IllegalStateException("Unknown view type $viewType")
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is HeaderViewHolder -> holder.bind(getItem(position) as? DailyWeatherListItem.Header)
      is DailyWeatherViewHolder -> holder.bind(getItem(position) as? DailyWeatherListItem.Weather)
      else -> throw IllegalStateException("Unknown type of view holder $holder at position $position")
    }
  }

  class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewDate = itemView.textViewDate!!

    fun bind(header: DailyWeatherListItem.Header?) = header?.date?.let {
      val current = Calendar.getInstance()
        .apply { time = time.trim() }
      calendar.time = it

      if (current == calendar) {
        textViewDate.text = "Today"
        return@let
      }

      current.add(Calendar.DATE, 1)
      if (current == calendar) {
        textViewDate.text = "Tomorrow"
        return@let
      }

      textViewDate.text = headerDateFormat.format(it)
    }
  }

  inner class DailyWeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textWeather = itemView.text_weather!!
    private val imageIconCityItem = itemView.image_icon_city_item!!
    private val textViewDataTime = itemView.textViewDataTime!!
    private val textTempMax = itemView.text_temp_max!!
    private val textTempMin = itemView.text_temp_min!!

    fun bind(weather: DailyWeatherListItem.Weather?) = weather?.let {
      textTempMin.text = weather.temperatureMin
      textTempMax.text = weather.temperatureMax

      textWeather.text =
          weather.weatherDescription
      textViewDataTime.text = itemDateFormat.format(weather.dataTime)

      Glide.with(itemView.context)
        .load(itemView.context.getIconDrawableFromDailyWeather(weather.weatherIcon))
        .apply(RequestOptions.fitCenterTransform().centerCrop())
        .transition(DrawableTransitionOptions.withCrossFade())
        .apply(
          ContextCompat.getColor(
            itemView.context,
            R.color.colorPrimaryDark
          )
            .let(::ColorDrawable)
            .let(RequestOptions::placeholderOf)
        )
        .into(imageIconCityItem)
    }
  }

  @IntDef(value = [HEADER_TYPE, DAILY_WEATHER_TYPE])
  @Retention(value = AnnotationRetention.SOURCE)
  annotation class ViewType

  companion object {
    @SuppressLint("SimpleDateFormat")
    @JvmField
    val headerDateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy, E")

    @SuppressLint("SimpleDateFormat")
    @JvmField
    val itemDateFormat: DateFormat = SimpleDateFormat("HH:mm")

    @JvmField
    val calendar = Calendar.getInstance()!!

    const val HEADER_TYPE = 1
    const val DAILY_WEATHER_TYPE = 3
  }
}