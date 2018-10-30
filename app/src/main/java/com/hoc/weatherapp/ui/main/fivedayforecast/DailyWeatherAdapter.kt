package com.hoc.weatherapp.ui.main.fivedayforecast

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.trim
import com.hoc.weatherapp.utils.ui.HeaderItemDecoration
import com.hoc.weatherapp.utils.ui.getIconDrawableFromDailyWeather
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.daily_weather_header_layout.view.*
import kotlinx.android.synthetic.main.daily_weather_item_layout.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DailyWeatherAdapter : ListAdapter<DailyWeatherListItem, RecyclerView.ViewHolder>(
  object : DiffUtil.ItemCallback<DailyWeatherListItem>() {
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
  }
), HeaderItemDecoration.StickyHeaderInterface {
  override val headerLayout = R.layout.daily_weather_header_layout

  override fun getHeaderPositionForItem(itemPosition: Int): Int {
    return (itemPosition downTo 0).find { isHeader(it) } ?: 0
  }

  override fun bindHeaderData(header: View, headerPosition: Int) {
    val textViewDate = header.textViewDate!!
    val headerItem = getItem(headerPosition) as? DailyWeatherListItem.Header ?: return
    bindHeader(textViewDate, headerItem)
    header.setBackgroundColor(ContextCompat.getColor(header.context, R.color.colorHeaderBackground))
    textViewDate.setTextColor(ContextCompat.getColor(header.context, R.color.colorHeaderText))
  }

  override fun isHeader(itemPosition: Int): Boolean {
    return getItem(itemPosition) is DailyWeatherListItem.Header
  }

  private val _clickSubject = PublishSubject.create<DailyWeatherListItem.Weather>()
  val clickObservable get() = _clickSubject.hide()!!

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
      is HeaderViewHolder -> holder.bind(getItem(position) as DailyWeatherListItem.Header)
      is DailyWeatherViewHolder -> holder.bind(getItem(position) as DailyWeatherListItem.Weather)
      else -> throw IllegalStateException("Unknown type of view holder $holder at position $position")
    }
  }

  class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewDate = itemView.textViewDate!!

    fun bind(header: DailyWeatherListItem.Header) = bindHeader(textViewDate, header)
  }

  inner class DailyWeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {
    override fun onClick(v: View) {
      val adapterPosition = adapterPosition
      if (adapterPosition != RecyclerView.NO_POSITION) {
        _clickSubject.onNext(getItem(adapterPosition) as DailyWeatherListItem.Weather)
      }
    }

    private val textWeather = itemView.text_weather!!
    private val imageIconCityItem = itemView.image_icon_city_item!!
    private val textViewDataTime = itemView.textViewDataTime!!
    private val textTempMax = itemView.text_temp_max!!
    private val textTempMin = itemView.text_temp_min!!

    init {
      itemView.setOnClickListener(this)
    }

    fun bind(weather: DailyWeatherListItem.Weather) {
      textTempMin.text = weather.temperatureMin
      textTempMax.text = weather.temperatureMax
      textWeather.text = weather.weatherDescription
      textViewDataTime.text = ITEM_DATE_FORMAT.format(weather.dataTime)

      Glide.with(itemView.context)
        .load(itemView.context.getIconDrawableFromDailyWeather(weather.weatherIcon))
        .apply(RequestOptions.fitCenterTransform().centerCrop())
        .transition(DrawableTransitionOptions.withCrossFade())
        .apply(
          ContextCompat.getColor(
            itemView.context,
            R.color.colorPrimaryDark
          ).let(::ColorDrawable)
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
    val HEADER_DATE_FORMAT: DateFormat = SimpleDateFormat("dd/MM/yyyy, E")

    @SuppressLint("SimpleDateFormat")
    @JvmField
    val ITEM_DATE_FORMAT: DateFormat = SimpleDateFormat("HH:mm")

    @JvmField
    val CALENDAR = Calendar.getInstance()!!

    const val HEADER_TYPE = 1
    const val DAILY_WEATHER_TYPE = 3

    private fun bindHeader(textView: TextView, headerItem: DailyWeatherListItem.Header) {
      val current = Calendar.getInstance()
        .apply { time = time.trim() }
      CALENDAR.time = headerItem.date

      if (current == CALENDAR) {
        textView.text = textView.context.getString(R.string.today)
        return
      }

      current.add(Calendar.DATE, 1)
      if (current == CALENDAR) {
        textView.text = textView.context.getString(R.string.tomorrow)
        return
      }

      textView.text = HEADER_DATE_FORMAT.format(headerItem.date)
    }
  }
}