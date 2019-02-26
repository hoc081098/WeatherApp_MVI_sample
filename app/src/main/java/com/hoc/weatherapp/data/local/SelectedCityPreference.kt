package com.hoc.weatherapp.data.local

import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.delegate
import com.hoc.weatherapp.utils.getOrNull
import com.hoc.weatherapp.utils.toOptional
import com.squareup.moshi.Moshi
import io.reactivex.subjects.BehaviorSubject

class SelectedCityPreference(sharedPreferences: SharedPreferences, private val moshi: Moshi) :
  PreferenceInterface<Optional<City>> {
  private var selectedCityJsonString by sharedPreferences.delegate<String>()
  private val citySubject =
    BehaviorSubject.createDefault(getSelectedCityFromSharedPref().toOptional())

  private fun getSelectedCityFromSharedPref(): City? {
    return runCatching {
      moshi
        .adapter(City::class.java)
        .fromJson(selectedCityJsonString)
    }.getOrNull()
  }

  /**
   * Save [value] to shared preference
   * @param value
   */
  @WorkerThread
  override fun save(value: Optional<City>) {
    selectedCityJsonString = moshi
      .adapter(City::class.java)
      .toJson(value.getOrNull())
    citySubject.onNext(value)
  }

  override val observable get() = citySubject.hide()!!
  override val value get() = citySubject.value ?: getSelectedCityFromSharedPref().toOptional()
}