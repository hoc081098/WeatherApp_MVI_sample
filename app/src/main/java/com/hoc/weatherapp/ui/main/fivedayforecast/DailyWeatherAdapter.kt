package com.hoc.weatherapp.ui.main.fivedayforecast

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.R
import com.hoc.weatherapp.databinding.DailyWeatherHeaderLayoutBinding
import com.hoc.weatherapp.databinding.DailyWeatherItemLayoutBinding
import com.hoc.weatherapp.utils.asObservable
import com.hoc.weatherapp.utils.ui.HeaderItemDecoration
import com.hoc.weatherapp.utils.ui.getIconDrawableFromDailyWeather
import com.hoc081098.viewbindingdelegate.inflateViewBinding
import io.reactivex.subjects.PublishSubject
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class DailyWeatherAdapter :
  ListAdapter<DailyWeatherListItem, RecyclerView.ViewHolder>(
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
  ),
  HeaderItemDecoration.StickyHeaderInterface<DailyWeatherHeaderLayoutBinding> {
  override fun viewBinding(parent: RecyclerView): DailyWeatherHeaderLayoutBinding =
    parent inflateViewBinding false

  override fun getHeaderPositionForItem(itemPosition: Int): Int? {
    if (itemPosition == 0) return null
    return (itemPosition downTo 0).find { isHeader(it) } ?: 0
  }

  override fun bindHeaderData(binding: DailyWeatherHeaderLayoutBinding, headerPosition: Int) =
    binding.run {
      val headerItem = getItem(headerPosition) as? DailyWeatherListItem.Header ?: return

      val weather =
        (
          runCatching { getItem(headerPosition - 1) }.getOrNull() as? DailyWeatherListItem.Weather
            ?: runCatching { getItem(headerPosition + 1) }.getOrNull() as? DailyWeatherListItem.Weather
          )

      bindHeader(
        textView = textViewDate,
        headerItem = headerItem,
        itemView = root,
        iconBackgroundColor = weather?.colors?.first
      )
      textViewDate.setTextColor(ContextCompat.getColor(root.context, R.color.colorHeaderText))
    }

  override fun isHeader(itemPosition: Int): Boolean =
    getItem(itemPosition) is DailyWeatherListItem.Header

  private val _clickSubject = PublishSubject.create<DailyWeatherListItem.Weather>()
  val clickObservable = _clickSubject.asObservable()

  @ViewType
  override fun getItemViewType(position: Int) = when (getItem(position)) {
    is DailyWeatherListItem.Header -> HEADER_TYPE
    is DailyWeatherListItem.Weather -> DAILY_WEATHER_TYPE
  }

  override fun onCreateViewHolder(parent: ViewGroup, @ViewType viewType: Int) = when (viewType) {
    HEADER_TYPE -> HeaderViewHolder(parent inflateViewBinding false)
    DAILY_WEATHER_TYPE -> DailyWeatherViewHolder(parent inflateViewBinding false)
    else -> error("Unknown view type $viewType")
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = when (holder) {
    is HeaderViewHolder -> holder.bind(getItem(position) as DailyWeatherListItem.Header)
    is DailyWeatherViewHolder -> holder.bind(getItem(position) as DailyWeatherListItem.Weather)
    else -> throw IllegalStateException("Unknown type of view holder $holder at position $position")
  }

  class HeaderViewHolder(private val binding: DailyWeatherHeaderLayoutBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(header: DailyWeatherListItem.Header) =
      bindHeader(
        textView = binding.textViewDate,
        headerItem = header,
        itemView = itemView,
      )
  }

  inner class DailyWeatherViewHolder(private val binding: DailyWeatherItemLayoutBinding) :
    RecyclerView.ViewHolder(binding.root),
    View.OnClickListener {

    override fun onClick(v: View) {
      val adapterPosition = adapterPosition
      if (adapterPosition != RecyclerView.NO_POSITION) {
        _clickSubject.onNext(getItem(adapterPosition) as DailyWeatherListItem.Weather)
      }
    }

    init {
      itemView.setOnClickListener(this)
    }

    fun bind(weather: DailyWeatherListItem.Weather) = binding.run {
      textTempMin.text = weather.temperatureMin
      textTempMax.text = weather.temperatureMax
      textWeather.text = weather.weatherDescription
      textViewDataTime.text = ITEM_DATE_FORMATTER.format(weather.dataTime)
      imageIconCityItem.setBackgroundColor(weather.colors.first)

      Glide.with(itemView.context)
        .load(itemView.context.getIconDrawableFromDailyWeather(weather.weatherIcon))
        .apply(RequestOptions.fitCenterTransform().centerCrop())
        .transition(DrawableTransitionOptions.withCrossFade())
        .apply(ColorDrawable(weather.colors.first).let(RequestOptions::placeholderOf))
        .into(imageIconCityItem)

      Unit
    }
  }

  @IntDef(value = [HEADER_TYPE, DAILY_WEATHER_TYPE])
  @Retention(value = AnnotationRetention.SOURCE)
  annotation class ViewType

  companion object {
    private val HEADER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy, E")
    private val ITEM_DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    private const val HEADER_TYPE = 1
    private const val DAILY_WEATHER_TYPE = 3

    @JvmStatic
    private fun bindHeader(
      textView: TextView,
      headerItem: DailyWeatherListItem.Header,
      itemView: View,
      @ColorInt iconBackgroundColor: Int? = null
    ) {
      /**
       * Set background color
       */
      @ColorInt val bgColor = iconBackgroundColor
        ?.let { ColorUtils.setAlphaComponent(it, 0xE6) }
        ?: Color.TRANSPARENT
      itemView.setBackgroundColor(bgColor)

      /**
       * Set text
       */
      val headerDate = headerItem.date
      val headerLocaleDate = headerDate.toLocalDate()
      val nowLocaleDate = ZonedDateTime.now(headerDate.zone).toLocalDate()

      textView.text = when {
        nowLocaleDate == headerLocaleDate -> textView.context.getString(R.string.today)
        nowLocaleDate.plusDays(1) == headerLocaleDate -> textView.context.getString(R.string.tomorrow)
        else -> headerDate.format(HEADER_DATE_FORMATTER)
      }
    }
  }
}
