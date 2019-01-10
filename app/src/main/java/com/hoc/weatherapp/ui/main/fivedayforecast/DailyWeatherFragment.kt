package com.hoc.weatherapp.ui.main.fivedayforecast

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.RefreshIntent
import com.hoc.weatherapp.ui.main.fivedayforecast.DetailDialog.Companion.TAG
import com.hoc.weatherapp.utils.debug
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
import kotlinx.android.synthetic.main.detail_item_layout.view.*
import kotlinx.android.synthetic.main.dialog_detail_daily_weather.view.*
import kotlinx.android.synthetic.main.fragment_daily_weather.*
import org.koin.android.ext.android.get
import java.text.SimpleDateFormat
import java.util.*

class DetailDialog : DialogFragment() {
  companion object {
    const val TAG = "com.hoc.weatherapp.ui.main.fivedayforecast.detail_dialog_tag"
    private const val ITEM_KEY = "com.hoc.weatherapp.ui.main.fivedayforecast.detail_dialog_item"

    fun newInstance(item: DailyWeatherListItem.Weather): DetailDialog {
      return DetailDialog().apply {
        arguments = Bundle().apply {
          putParcelable(ITEM_KEY, item)
        }
      }
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val item = arguments?.getParcelable<DailyWeatherListItem.Weather>(ITEM_KEY)
      ?: return super.onCreateDialog(savedInstanceState)

    val view =
      LayoutInflater.from(requireContext()).inflate(R.layout.dialog_detail_daily_weather, null)
    view.image_icon.setImageResource(requireContext().getIconDrawableFromDailyWeather(item.weatherIcon))
    view.text_data_time.text = SimpleDateFormat(
      "dd/MM/yyyy, HH:mm",
      Locale.getDefault()
    ).format(item.dataTime)
    view.text_main.text = item.main
    view.text_description.text = item.weatherDescription

    view.recycler_detail.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(requireContext())
      adapter = object : RecyclerView.Adapter<DailyWeatherFragment.VH>() {
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
            .let(DailyWeatherFragment::VH)

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: DailyWeatherFragment.VH, position: Int) =
          holder.bind(list[position])
      }
    }

    return AlertDialog.Builder(requireContext())
      .setMessage("Detail")
      .setView(view)
      .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
      .show()
  }
}

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
      .subscribeBy(onNext = ::showDetailDialog)
      .addTo(compositeDisposable)
  }

  private fun showDetailDialog(item: DailyWeatherListItem.Weather) {
    (fragmentManager?.findFragmentByTag(TAG) as? DetailDialog)?.dismiss()
    DetailDialog.newInstance(item).show(fragmentManager, TAG)
  }

  override fun onPause() {
    super.onPause()
    (fragmentManager?.findFragmentByTag(TAG) as? DetailDialog)?.dismiss()
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

  class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val image = itemView.imageView5
    private val text = itemView.textView

    fun bind(pair: Pair<Int, String>) {
      Glide.with(itemView)
        .load(pair.first)
        .apply(RequestOptions.fitCenterTransform().centerCrop())
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(image)
      text.text = pair.second
    }
  }
}
