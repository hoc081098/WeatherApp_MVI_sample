package com.hoc.weatherapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions.fitCenterTransform
import com.bumptech.glide.request.RequestOptions.placeholderOf
import com.hoc.weatherapp.AddCityActivity.Companion.ACTION_CHANGED_LOCATION
import com.hoc.weatherapp.AddCityActivity.Companion.SELECTED_CITY
import com.hoc.weatherapp.data.WeatherRepository
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.utils.SwipeController
import com.hoc.weatherapp.utils.SwipeControllerActions
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.getIconDrawableFromIconString
import com.hoc.weatherapp.utils.toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_location.*
import kotlinx.android.synthetic.main.city_item_layout.view.*
import org.koin.android.ext.android.inject

class CityAdapter(selectedCityId: Long?, private val onClickListener: (City) -> Unit) :
    ListAdapter<CurrentWeather, CityAdapter.ViewHolder>(diffCallback) {
    var selectedCityId: Long? = selectedCityId
        set(value) {
            field = value
            notifyDataSetChanged()
            debug("Change selectedCityId => $value")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.city_item_layout, parent, false)
            .let(::ViewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val textName = itemView.text_name!!
        private val textWeather = itemView.text_weather
        private val imageIconCityItem = itemView.image_icon_city_item
        private val radioButtonSelectedCity = itemView.radio_button_selected_city

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                position.let(::getItem)
                    .let(CurrentWeather::city)
                    .let(onClickListener)
            }
        }

        fun bind(weather: CurrentWeather) {
            textName.text = "${weather.city.name} - ${weather.city.country}"
            textWeather.text =
                "${weather.description.capitalize()}, ${weather.temperatureMin} ℃ ~ ${weather.temperatureMax} ℃"
            radioButtonSelectedCity.isChecked = weather.city.id == selectedCityId


            Glide.with(itemView.context)
                .load(getIconDrawableFromIconString(weather.icon))
                .apply(fitCenterTransform().centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(
                    ContextCompat.getColor(itemView.context, R.color.colorPrimaryDark)
                        .let(::ColorDrawable)
                        .let(::placeholderOf)
                )
                .into(imageIconCityItem)
        }
    }

    companion object {
        @JvmField
        val diffCallback = object : DiffUtil.ItemCallback<CurrentWeather>() {
            override fun areItemsTheSame(oldItem: CurrentWeather, newItem: CurrentWeather) =
                oldItem.city.id == newItem.city.id

            override fun areContentsTheSame(oldItem: CurrentWeather, newItem: CurrentWeather) =
                oldItem == newItem
        }
    }
}

class LocationActivity : AppCompatActivity() {
    private lateinit var cityAdapter: CityAdapter
    private val weatherRepository by inject<WeatherRepository>()
    private val compositeDisposable = CompositeDisposable()
    private val compositeDisposeble1 = CompositeDisposable()
    private val broadcastReceiver = LocationActivityBroadcastReceiver()
    private val sharedPrefUtil by inject<SharedPrefUtil>()
    private var weathers: List<CurrentWeather> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = "City"
        }

        fab.setOnClickListener {
            startActivity(Intent(this, AddCityActivity::class.java))
        }

        recycler_city.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LocationActivity)

            cityAdapter = CityAdapter(sharedPrefUtil.selectedCity?.id, ::onItemCityClick)
            adapter = cityAdapter

            addItemDecoration(DividerItemDecoration(this@LocationActivity, VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        fab.hide()
                    } else {
                        fab.show()
                    }
                }
            })

            // swipe
            val swipeController = SwipeController(object : SwipeControllerActions {
                override fun onLeftClicked(adapterPosition: Int) {
                    if (adapterPosition in weathers.indices) {
                        refreshWeatherOfCity(weathers[adapterPosition].city)
                    }
                }

                override fun onRightClicked(adapterPosition: Int) {
                    if (adapterPosition in weathers.indices) {
                        deleteWeather(weathers[adapterPosition], adapterPosition)
                    }
                }
            })
            ItemTouchHelper(swipeController).attachToRecyclerView(this)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                    swipeController.onDraw(c)
                }
            })
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                broadcastReceiver,
                IntentFilter().apply {
                    addAction(ACTION_CHANGED_LOCATION)
                }
            )
    }

    private fun deleteWeather(weather: CurrentWeather, adapterPosition: Int) {
        val newSelectedCity = weathers.getOrNull(adapterPosition + 1)?.city
            ?: weathers.getOrNull(adapterPosition - 1)?.city

        weatherRepository.deleteWeather(weather)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {
                    toast("Delete ${weather.city.name} failed: ${it.message}")
                },
                onComplete = {
                    if (cityAdapter.selectedCityId == weather.city.id) {
                        onItemCityClick(newSelectedCity)
                    }
                    toast("Delete ${weather.city.name} successfully")
                }
            )
            .addTo(compositeDisposeble1)
    }

    private fun refreshWeatherOfCity(city: City) {
        weatherRepository.getCurrentWeatherByCity(city)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {
                    toast("Refresh weather of ${city.name} failed: ${it.message}")
                },
                onComplete = {
                    toast("Refresh weather of ${city.name} successfully")
                }
            )
            .addTo(compositeDisposeble1)
    }

    private fun onItemCityClick(city: City?) {
        cityAdapter.selectedCityId = city?.id
        sharedPrefUtil.selectedCity = city

        LocalBroadcastManager.getInstance(this@LocationActivity)
            .sendBroadcast(
                Intent(ACTION_CHANGED_LOCATION).apply {
                    putExtra(SELECTED_CITY, city)
                    putExtra("SELF", true)
                }
            )
    }

    override fun onStart() {
        super.onStart()
        weatherRepository.getAllWeathers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {},
                onNext = {
                    cityAdapter.submitList(it)
                    weathers = it
                }
            )
            .addTo(compositeDisposable)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)
        compositeDisposeble1.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> return super.onOptionsItemSelected(item)

        }
    }

    inner class LocationActivityBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CHANGED_LOCATION -> {
                    if (!intent.getBooleanExtra("SELF", false)) {
                        val city = intent.getParcelableExtra<City>(SELECTED_CITY)
                        cityAdapter.selectedCityId = city?.id
                        sharedPrefUtil.selectedCity = city
                    }
                }
            }
        }
    }
}
