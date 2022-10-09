package com.hoc.weatherapp.ui.main.fivedayforecast

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hoc.weatherapp.R
import com.hoc.weatherapp.databinding.FragmentDailyWeatherBinding
import com.hoc.weatherapp.ui.BaseMviFragment
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.RefreshIntent
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.snackBar
import com.hoc.weatherapp.utils.ui.HeaderItemDecoration
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.get

@ExperimentalStdlibApi
class DailyWeatherFragment :
  BaseMviFragment<DailyWeatherContract.View, DailyWeatherPresenter>(R.layout.fragment_daily_weather),
  DailyWeatherContract.View {
  private val binding by viewBinding<FragmentDailyWeatherBinding>()

  private var errorSnackBar: Snackbar? = null
  private var refreshSnackBar: Snackbar? = null

  private val dailyWeatherAdapter = DailyWeatherAdapter()
  private val compositeDisposable = CompositeDisposable()
  private val initialRefreshSubject = PublishSubject.create<RefreshIntent.InitialRefreshIntent>()

  override fun refreshDailyWeatherIntent(): Observable<RefreshIntent> {
    return binding.swipeRefreshLayout.refreshes()
      .map { RefreshIntent.UserRefreshIntent }
      .cast<RefreshIntent>()
      .mergeWith(initialRefreshSubject)
      .doOnNext { debug("refreshDailyWeatherIntent", "_daily_weather_") }
  }

  override fun createPresenter() = requireActivity().get<DailyWeatherPresenter>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.recyclerDailyWeathers.run {
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
    binding.swipeRefreshLayout.isRefreshing = false

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
