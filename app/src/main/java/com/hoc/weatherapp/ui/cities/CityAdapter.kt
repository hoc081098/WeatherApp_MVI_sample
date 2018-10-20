package com.hoc.weatherapp.ui.cities

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.R
import com.hoc.weatherapp.utils.getIconDrawableFromCurrentWeather
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.city_item_layout.view.*

class CitiesAdapter : ListAdapter<CityListItem, CitiesAdapter.ViewHolder>(object :
  DiffUtil.ItemCallback<CityListItem>() {
  override fun areItemsTheSame(oldItem: CityListItem, newItem: CityListItem): Boolean {
    return oldItem.city.id == newItem.city.id
  }

  override fun areContentsTheSame(oldItem: CityListItem, newItem: CityListItem): Boolean {
    return oldItem == newItem
  }
}) {
  private val _itemClickSubject = PublishSubject.create<Pair<Int, CityListItem>>()
  val itemClickObservable get() = _itemClickSubject.hide()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    LayoutInflater.from(parent.context)
      .inflate(R.layout.city_item_layout, parent, false)
      .let(::ViewHolder)

  override fun onBindViewHolder(holder: CitiesAdapter.ViewHolder, position: Int) =
    holder.bind(getItem(position))

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {
    private val textName = itemView.text_name!!
    private val textWeather = itemView.text_weather
    private val imageIconCityItem = itemView.image_icon_city_item
    private val radioButtonSelectedCity = itemView.radio_button_selected_city

    init {
      itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
      val position = adapterPosition
      if (position != NO_POSITION) {
        _itemClickSubject.onNext(position to getItem(position))
      }
    }

    fun bind(item: CityListItem) = item.run {
      textName.text = itemView.context.getString(
        R.string.city_name_and_country,
        city.name,
        city.country
      )
      textWeather.text =
          "${weatherDescription.capitalize()}, ${temperatureMin} ~ ${temperatureMax}"
      radioButtonSelectedCity.isChecked = isSelected

      Glide.with(itemView.context)
        .load(
          itemView.context.getIconDrawableFromCurrentWeather(
            weatherConditionId = weatherConditionId,
            weatherIcon = weatherIcon
          )
        )
        .apply(RequestOptions.fitCenterTransform().centerCrop())
        .transition(DrawableTransitionOptions.withCrossFade())
        .apply(
          ContextCompat.getColor(
            itemView.context,
            R.color.colorPrimaryDark
          ).let(::ColorDrawable).let(RequestOptions::placeholderOf)
        )
        .into(imageIconCityItem)
      Unit
    }

  }
}
