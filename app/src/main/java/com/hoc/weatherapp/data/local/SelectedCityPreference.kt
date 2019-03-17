package com.hoc.weatherapp.data.local

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.asObservable
import com.hoc.weatherapp.utils.delegate
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.toOptional
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

@SuppressLint("CheckResult")
class SelectedCityPreference(sharedPreferences: SharedPreferences, private val moshi: Moshi) :
  PreferenceInterface<Optional<City>> {
  private var selectedCityJsonString by sharedPreferences.delegate<String>()
  private val citySubject = BehaviorSubject.createDefault<Optional<City>>(None)

  init {
    Single
      .fromCallable(::getSelectedCityFromSharedPref)
      .subscribeOn(Schedulers.single())
      .onErrorReturnItem(None)
      .subscribeBy(onSuccess = citySubject::onNext)
  }

  @WorkerThread private fun getSelectedCityFromSharedPref(): Optional<City> {
    return runCatching {
      moshi
        .adapter(City::class.java)
        .fromJson(selectedCityJsonString)
    }.getOrNull().toOptional()
  }

  /**
   * Save [value] to shared preference
   * @param value
   */
  @WorkerThread override fun save(value: Optional<City>) {
    selectedCityJsonString = moshi
      .adapter(City::class.java)
      .toJson(value.getOrNull())
    citySubject.onNext(value)
  }

  override val observable = citySubject.asObservable()

  override val value @WorkerThread get() = getSelectedCityFromSharedPref()
}

