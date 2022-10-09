package com.hoc.weatherapp.ui.cities

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.databinding.CityItemLayoutBinding
import com.hoc.weatherapp.utils.themeColor
import com.hoc.weatherapp.utils.ui.getIconDrawableFromCurrentWeather
import com.hoc081098.viewbindingdelegate.inflateViewBinding
import io.reactivex.subjects.PublishSubject
import java.util.Locale
import org.threeten.bp.format.DateTimeFormatter

@ExperimentalStdlibApi
class CitiesAdapter : ListAdapter<CityListItem, CitiesAdapter.ViewHolder>(object :
    DiffUtil.ItemCallback<CityListItem>() {
    override fun areItemsTheSame(oldItem: CityListItem, newItem: CityListItem): Boolean {
      return oldItem.city.id == newItem.city.id
    }

    override fun areContentsTheSame(oldItem: CityListItem, newItem: CityListItem): Boolean {
      return oldItem == newItem
    }

    override fun getChangePayload(oldItem: CityListItem, newItem: CityListItem): Any? {
      return when {
        newItem.sameExceptIsSelected(oldItem) -> newItem.isSelected
        else -> null
      }
    }
  }) {
  private val _itemClickSubject = PublishSubject.create<City>()
  val itemClickObservable get() = _itemClickSubject.hide()!!

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    ViewHolder(parent inflateViewBinding false)

  override fun onBindViewHolder(holder: ViewHolder, position: Int) =
    holder.bind(getItem(position))

  override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
    val isSelected = payloads.firstOrNull() as? Boolean ?: return onBindViewHolder(holder, position)
    holder.updateRadio(isSelected)
  }

  inner class ViewHolder(private val binding: CityItemLayoutBinding) :
    RecyclerView.ViewHolder(binding.root),
    View.OnClickListener {
    init {
      itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
      val position = adapterPosition
      if (position != NO_POSITION) {
        _itemClickSubject.onNext(getItem(position).city)
      }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: CityListItem) = binding.run {
      textName.text = itemView.context.getString(
        R.string.city_name_and_country,
        item.city.name,
        item.city.country
      )
      textMain.text = item.weatherDescription.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
          Locale.ROOT
        ) else it.toString()
      }
      updateRadio(item.isSelected)
      textLastUpdated.text =
        "${item.lastUpdated.format(TIME_FORMATTER)} (${item.lastUpdated.zone.id}): "
      textTemps.text = "${item.temperatureMin} ~ ${item.temperatureMax}"

      Glide.with(itemView.context)
        .load(
          itemView.context.getIconDrawableFromCurrentWeather(
            weatherConditionId = item.weatherConditionId,
            weatherIcon = item.weatherIcon
          )
        )
        .apply(RequestOptions.fitCenterTransform().centerCrop())
        .transition(DrawableTransitionOptions.withCrossFade())
        .apply(
          itemView.context
            .themeColor(R.attr.colorSecondary)
            .let(::ColorDrawable)
            .let(RequestOptions::placeholderOf)
        )
        .into(imageIconCityItem)
      Unit
    }

    fun updateRadio(isSelected: Boolean) {
      binding.radioButtonSelectedCity.isChecked = isSelected
    }
  }

  companion object {
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yy")
  }
}
