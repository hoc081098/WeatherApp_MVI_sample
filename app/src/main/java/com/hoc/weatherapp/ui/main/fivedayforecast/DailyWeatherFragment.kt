package com.hoc.weatherapp.ui.main.fivedayforecast

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.ui.BaseAppCompatActivity
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.RefreshIntent
import com.hoc.weatherapp.utils.Mode
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.setColorFilter
import com.hoc.weatherapp.utils.snackBar
import com.hoc.weatherapp.utils.ui.HeaderItemDecoration
import com.hoc.weatherapp.utils.ui.getIconDrawableFromDailyWeather
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_detail_daily_weather.*
import kotlinx.android.synthetic.main.detail_item_layout.view.*
import kotlinx.android.synthetic.main.fragment_daily_weather.*
import org.koin.android.ext.android.get
import org.threeten.bp.format.DateTimeFormatter

class DailyDetailActivity : BaseAppCompatActivity() {
  companion object {
    const val TAG = "com.hoc.weatherapp.ui.main.fivedayforecast.daily_detail_activity"
    const val ITEM_KEY =
        "com.hoc.weatherapp.ui.main.fivedayforecast.daily_detail_activity_item"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    setContentView(R.layout.activity_detail_daily_weather)

    setSupportActionBar(toolbar)
    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(R.drawable.ic_navigate_before_black_24dp)
    }

    bind(intent.getParcelableExtra(ITEM_KEY)!!)
  }

  private fun bind(item: DailyWeatherListItem.Weather) {
    item.colors.second.let {
      window.statusBarColor = it
      divider.setBackgroundColor(it)
      text_data_time.setTextColor(it)
    }


    image_icon.setImageResource(getIconDrawableFromDailyWeather(item.weatherIcon))
    image_icon.setBackgroundColor(item.colors.second)
    text_data_time.text = item.dataTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"))
    text_main.text = item.main
    text_description.text = item.weatherDescription

    recycler_detail.run {
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
            LayoutInflater.from(parent.context)
                .inflate(R.layout.detail_item_layout, parent, false)
                .let(::VH)

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

  class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val image = itemView.imageView5
    private val text = itemView.textView

    fun bind(pair: Pair<Int, String>, @ColorInt iconBackgroundColor: Int) {
      itemView.context.getDrawable(pair.first)!!.mutate()
          .apply { setColorFilter(iconBackgroundColor, Mode.SRC_IN) }
          .let { image.setImageDrawable(it) }
      text.text = pair.second
    }
  }
}

@ExperimentalStdlibApi
class DailyWeatherFragment : MviFragment<DailyWeatherContract.View, DailyWeatherPresenter>(),
    DailyWeatherContract.View {

  private var errorSnackBar: Snackbar? = null
  private var refreshSnackBar: Snackbar? = null

  private val dailyWeatherAdapter = DailyWeatherAdapter()
  private val compositeDisposable = CompositeDisposable()
  private val initialRefreshSubject = PublishSubject.create<RefreshIntent.InitialRefreshIntent>()

  override fun refreshDailyWeatherIntent(): Observable<RefreshIntent> {
    return swipe_refresh_layout.refreshes()
        .map { RefreshIntent.UserRefreshIntent }
        .cast<RefreshIntent>()
        .mergeWith(initialRefreshSubject)
        .doOnNext { debug("refreshDailyWeatherIntent", "_daily_weather_") }
  }

  override fun createPresenter() = get<DailyWeatherPresenter>()

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
      addItemDecoration(HeaderItemDecoration(dailyWeatherAdapter))
    }
  }

  override fun onResume() {
    super.onResume()
    initialRefreshSubject.onNext(RefreshIntent.InitialRefreshIntent)
    dailyWeatherAdapter
        .clickObservable
        .subscribeBy(onNext = ::showDetail)
        .addTo(compositeDisposable)
  }

  private fun showDetail(item: DailyWeatherListItem.Weather) {
    val context = requireContext()
    context.startActivity(
        Intent(context, DailyDetailActivity::class.java).apply {
          putExtra(DailyDetailActivity.ITEM_KEY, item)
        }
    )
  }

  override fun onPause() {
    super.onPause()
    compositeDisposable.clear()
  }

  override fun render(viewState: DailyWeatherContract.ViewState) {
    swipe_refresh_layout.isRefreshing = false

    dailyWeatherAdapter.submitList(viewState.dailyWeatherListItem)

    if (viewState.error != null && viewState.showError) {
      errorSnackBar?.dismiss()
      errorSnackBar = view?.snackBar(viewState.error.message ?: "An error occurred")
    }
    if (!viewState.showError) {
      errorSnackBar?.dismiss()
    }

    if (viewState.showRefreshSuccessfully) {
      refreshSnackBar?.dismiss()
      refreshSnackBar = view?.snackBar("Refresh successfully")
    } else {
      refreshSnackBar?.dismiss()
    }
  }
}
