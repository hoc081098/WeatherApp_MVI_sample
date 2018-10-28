package com.hoc.weatherapp.ui.main.fivedayforecast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherContract.RefreshIntent
import com.hoc.weatherapp.utils.toast
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject
import kotlinx.android.synthetic.main.fragment_daily_weather.*

class DailyWeatherFragment : MviFragment<DailyWeatherContract.View, DailyWeatherPresenter>(),
  DailyWeatherContract.View {

  override fun refreshDailyWeatherIntent(): Observable<RefreshIntent> {
    return swipe_refresh_layout.refreshes()
      .map { RefreshIntent.UserIntent }
      .cast<RefreshIntent>()
      .mergeWith(initialRefreshSubject)
  }

  override fun createPresenter(): DailyWeatherPresenter {
    return DailyWeatherPresenter(repository = weatherRepository)
  }

  private val weatherRepository by inject<Repository>()
  private val initialRefreshSubject = PublishSubject.create<RefreshIntent.InitialIntent>()
  private val dailyWeatherAdapter = DailyWeatherAdapter()

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
          onError = { toast(it.message ?: "An error occurred") },
          onSuccess = { toast("Refresh successfully") }
        )
    }
  }

  override fun onResume() {
    super.onResume()
    initialRefreshSubject.onNext(RefreshIntent.InitialIntent)
  }

  override fun render(viewState: DailyWeatherContract.ViewState) {
    dailyWeatherAdapter.submitList(viewState.dailyWeatherListItem)
  }
}
