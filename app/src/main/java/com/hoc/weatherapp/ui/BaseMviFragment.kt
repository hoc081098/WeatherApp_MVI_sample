package com.hoc.weatherapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.hannesdorfmann.mosby3.mvi.MviPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView

abstract class BaseMviFragment<V : MvpView, P : MviPresenter<V, *>>(
  @LayoutRes private val contentLayoutId: Int
) : MviFragment<V, P>() {
  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(contentLayoutId, container, false)!!
}
