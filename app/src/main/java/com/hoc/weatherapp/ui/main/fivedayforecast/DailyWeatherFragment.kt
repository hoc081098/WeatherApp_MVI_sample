package com.hoc.weatherapp.ui.main.fivedayforecast

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.NoSelectedCityException
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.models.entity.DailyWeather
import com.hoc.weatherapp.utils.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.daily_weather_header_layout.view.*
import kotlinx.android.synthetic.main.daily_weather_item_layout.view.*
import kotlinx.android.synthetic.main.fragment_daily_weather.*
import org.koin.android.ext.android.inject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DailyWeatherAdapter(
  temperatureUnit: TemperatureUnit,
  val list: MutableList<Any> = mutableListOf()
) :
  RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  var temperatureUnit: TemperatureUnit = temperatureUnit
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun getItemCount(): Int {
    return list.size
  }

  @ViewType
  override fun getItemViewType(position: Int): Int {
    return when (
      val item = getItem(position)) {
      is Date -> HEADER_TYPE
      is DailyWeather -> DAILY_WEATHER_TYPE
      else -> throw  IllegalStateException("Unknown type of item $item at position $position")
    }
  }

  private fun getItem(position: Int): Any {
    return list[position]
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
      is HeaderViewHolder -> holder.bind(getItem(position) as? Date)
      is DailyWeatherViewHolder -> holder.bind(getItem(position) as? DailyWeather)
      else -> throw IllegalStateException("Unknown type of view holder $holder at position $position")
    }
  }

  class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewDate = itemView.textViewDate!!

    fun bind(date: Date?) = date?.let {
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

    fun bind(dailyWeather: DailyWeather?) = dailyWeather?.let { weather ->
      textTempMin.text = temperatureUnit.format(weather.temperatureMin)
      textTempMax.text = temperatureUnit.format(weather.temperatureMax)

      textWeather.text =
          weather.description.capitalize()
      textViewDataTime.text = itemDateFormat.format(weather.timeOfDataForecasted)

      Glide.with(itemView.context)
        .load(itemView.context.getIconDrawableFromDailyWeather(weather))
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

class DailyWeatherFragment : Fragment() {
  private val weatherRepository by inject<Repository>()
  private val sharedPrefUtil by inject<SharedPrefUtil>()

  private val compositeDisposable = CompositeDisposable()
  private val dailyWeatherAdapter = DailyWeatherAdapter(sharedPrefUtil.temperatureUnit)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_daily_weather, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recycler_daily_weathers.run {
      setHasFixedSize(true)
      val linearLayoutManager = LinearLayoutManager(context)
      layoutManager = linearLayoutManager
      adapter = dailyWeatherAdapter
      DividerItemDecoration(context, linearLayoutManager.orientation)
        .apply {
          ContextCompat.getDrawable(
            context,
            R.drawable.daily_weather_divider
          )?.let(::setDrawable)
        }
        .let(::addItemDecoration)
    }

    swipe_refresh_layout.setOnRefreshListener {
      weatherRepository.refreshFiveDayForecastOfSelectedCity()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess { swipe_refresh_layout.isRefreshing = false }
        .doOnError { swipe_refresh_layout.isRefreshing = false }
        .subscribeBy(
          onError = {toast(it.message ?: "An error occurred")},
          onSuccess = {toast("Refresh successfully")}
        )
    }
    getDailyWeather()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }

  private fun getDailyWeather() {
    weatherRepository.getFiveDayForecastOfSelectedCity()
      .publish { shared ->
        Flowable.mergeArray(
          shared.ofType<None>().map { throw NoSelectedCityException },
          shared.ofType<Some<List<DailyWeather>>>()
            .map { it.value }
            .map { dailyWeathers ->
              dailyWeathers.groupBy { it.timeOfDataForecasted.trim() }
                .toSortedMap()
                .flatMap { (date, weathers) -> listOf(date) + weathers }
            }
        ).map { Result.success(it) }
          .onErrorReturn { Result.failure(it) }
      }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { result ->
          result.onSuccess {
            dailyWeatherAdapter.list.run {
              clear()
              addAll(it)
            }
            dailyWeatherAdapter.notifyDataSetChanged()
          }
        }
      )
      .addTo(compositeDisposable)
  }
}