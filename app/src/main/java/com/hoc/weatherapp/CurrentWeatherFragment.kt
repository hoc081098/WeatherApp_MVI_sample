package com.hoc.weatherapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hoc.weatherapp.data.Weather
import com.hoc.weatherapp.utils.getIconDrawableFromIconString
import kotlinx.android.synthetic.main.fragment_current_weather.*
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class CurrentWeatherFragment : Fragment() {
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private lateinit var mainActivity: MainActivity

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = layoutInflater.inflate(R.layout.fragment_current_weather, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipe_refresh_layout.setOnRefreshListener {
            mainActivity.getCurrentWeather(sharedPrefUtil.selectedCity)
        }
    }

    fun updateUi(weather: Weather) {
        swipe_refresh_layout.post {
            swipe_refresh_layout.isRefreshing = false
        }
        updateWeatherIcon(weather.icon)
        text_temperature.text = "${weather.temperature} \u2103"
        text_main_weather.text = weather.main
        text_last_update.text = "${sdf.format(weather.dataTime)} update"
    }

    private fun updateWeatherIcon(icon: String) {
        Glide.with(this)
                .load(getIconDrawableFromIconString(icon))
                .apply(RequestOptions.fitCenterTransform().centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(image_icon)
    }

    companion object {
        @JvmField
        val sdf = SimpleDateFormat("dd/MM/yy hh:mm", Locale.US)
    }
}

