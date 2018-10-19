package com.hoc.weatherapp.ui

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions.fitCenterTransform
import com.bumptech.glide.request.RequestOptions.placeholderOf
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.Repository
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CityAndCurrentWeather
import com.hoc.weatherapp.data.remote.TemperatureUnit
import com.hoc.weatherapp.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_location.*
import kotlinx.android.synthetic.main.city_item_layout.view.*
import org.koin.android.ext.android.inject

class CityAdapter(
    selectedCityId: Long?,
    private val temperatureUnit: TemperatureUnit,
    private val onClickListener: (City) -> Unit
) :
    ListAdapter<CityAndCurrentWeather, CityAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<CityAndCurrentWeather>() {
        override fun areItemsTheSame(
            oldItem: CityAndCurrentWeather,
            newItem: CityAndCurrentWeather
        ): Boolean {
            return oldItem.city.id == newItem.city.id
        }

        override fun areContentsTheSame(
            oldItem: CityAndCurrentWeather,
            newItem: CityAndCurrentWeather
        ): Boolean {
            return oldItem == newItem
        }
    }) {
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
                    .city
                    .let(onClickListener)
            }
        }

        fun bind(weather: CityAndCurrentWeather) {
            val temperatureMin = temperatureUnit.format(weather.currentWeather.temperatureMin)
            val temperatureMax = temperatureUnit.format(weather.currentWeather.temperatureMax)

            textName.text = itemView.context.getString(
                R.string.city_name_and_country,
                weather.city.name,
                weather.city.country
            )
            textWeather.text =
                    "${weather.currentWeather.description.capitalize()}, $temperatureMin ~ $temperatureMax"
            radioButtonSelectedCity.isChecked = weather.city.id == selectedCityId

            Glide.with(itemView.context)
                .load(itemView.context.getIconDrawableFromCurrentWeather(weather.currentWeather))
                .apply(fitCenterTransform().centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorPrimaryDark
                    ).let(::ColorDrawable).let(::placeholderOf)
                )
                .into(imageIconCityItem)
        }
    }
}

class LocationsActivity : AppCompatActivity() {
    private val repository by inject<Repository>()
    private val sharedPrefUtil by inject<SharedPrefUtil>()

    private val compositeDisposable = CompositeDisposable()
    private val compositeDisposeble1 = CompositeDisposable()
    private val localBroadcastManager by lazy(LazyThreadSafetyMode.NONE) {
        LocalBroadcastManager.getInstance(this)
    }
//    private val broadcastReceiver = LocationActivityBroadcastReceiver()

    private var weathers: List<CityAndCurrentWeather> = emptyList()
    private lateinit var cityAdapter: CityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = "City"
        }

        fab.setOnClickListener {
            //            startActivity<AddCityActivity>()
            repository.getCityInformationByLatLng(14.0, 108.0)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { toast(it.message.toString());Log.d("@@@", it.message, it) },
                    onSuccess = { toast("Add done") }
                )
        }
        search_view.setHint("Search...")
        setupRecyclerViewCities()

//        localBroadcastManager.registerReceiver(
//            broadcastReceiver,
//            IntentFilter().apply {
//                addAction(ACTION_CHANGED_LOCATION)
//            }
//        )

        repository.getSelectedCity()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                when (it) {
                    is None -> toast("No selected city")
                    else -> toast("Has selected city")
                }
            }
    }

    private fun setupRecyclerViewCities() {
        recycler_city.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LocationsActivity)

            cityAdapter = CityAdapter(
                sharedPrefUtil.selectedCity?.id,
                sharedPrefUtil.temperatureUnit
            ) { onChangeSelectedCity(it) }
            adapter = cityAdapter

            addItemDecoration(DividerItemDecoration(this@LocationsActivity, VERTICAL))
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

//             swipe
            val swipeController = SwipeController(object : SwipeControllerActions {
                override fun onLeftClicked(adapterPosition: Int) {
//                    if (adapterPosition in weathers.indices) {
//                        refreshWeatherOfCity(weathers[adapterPosition].city)
//                    }
                }

                override fun onRightClicked(adapterPosition: Int) {
                    AlertDialog.Builder(this@LocationsActivity)
                        .setTitle("Delete city")
                        .setMessage("Do you want to delete this city")
                        .setIcon(R.drawable.ic_delete_black_24dp)
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .setPositiveButton("Ok") { dialog, _ ->
                            dialog.dismiss()
                            deleteWeather(
                                weathers.getOrNull(adapterPosition)?.city
                                    ?: return@setPositiveButton, adapterPosition
                            )
                        }
                        .show()
                }
            })
            ItemTouchHelper(swipeController).attachToRecyclerView(this)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                    swipeController.onDraw(c)
                }
            })
        }
    }

    private fun deleteWeather(city: City, adapterPosition: Int) {
        val newSelectedCity = weathers.getOrNull(adapterPosition + 1)?.city
            ?: weathers.getOrNull(adapterPosition - 1)?.city
        val selectedId = cityAdapter.selectedCityId

        repository.deleteCity(city)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {
                    Log.d("GOD_IS_LOVE", it.message, it)
                    //                    root_location_activity.snackBar("Delete ${city.name} failed: ${it.message}")
                },
                onComplete = {
                    /**
                     * If selected city is deleted, then newSelectedCity will be selected city
                     */
//                    if (selectedId == city.id) {
//                        onChangeSelectedCity(newSelectedCity)
//                    }
                    toast("Delete ${city.name} successfully")
                }
            )
            .addTo(compositeDisposeble1)
    }

    private fun refreshWeatherOfCity(city: City) {
//        weatherRepository.getCurrentWeatherByCity(city)
//            .subscribeOn(Schedulers.io())
//            .zipWith(
//                weatherRepository.getFiveDayForecastByCity(city)
//                    .subscribeOn(Schedulers.io())
//            )
//            .lastElement()
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribeBy(
//                onError = {
//                    root_location_activity.snackBar("Refresh weather of ${city.name} failed: ${it.message}")
//                },
//                onSuccess = { (current, daily) ->
//                    root_location_activity.snackBar("Refresh weather of ${city.name} successfully")
//
//                    val id = sharedPrefUtil.selectedCity?.id
//                    /**
//                     * If refresh selected city
//                     */
//                    if (current.city.id == id && daily.all { it.city.id == id }) {
//                        /**
//                         * Update city in shared preference
//                         */
//                        sharedPrefUtil.selectedCity = current.city
//
//                        /**
//                         * Send broadcast intent to CurrentWeatherFragment
//                         */
//                        localBroadcastManager
//                            .sendBroadcast(
//                                Intent(ACTION_UPDATE_CURRENT_WEATHER).apply {
//                                    putExtra(EXTRA_CURRENT_WEATHER, current)
//                                }
//                            )
//
//                        /**
//                         * Send broadcast intent to DailyWeatherAdapter
//                         */
//                        localBroadcastManager
//                            .sendBroadcast(
//                                Intent(ACTION_UPDATE_DAILY_WEATHERS).apply {
//                                    putParcelableArrayListExtra(
//                                        EXTRA_DAILY_WEATHERS,
//                                        ArrayList(daily)
//                                    )
//                                }
//                            )
//                    }
//                }
//            )
//            .addTo(compositeDisposeble1)
    }

    private fun onChangeSelectedCity(newSelectedCity: City?, sendBroadcast: Boolean = true) {
//        cityAdapter.selectedCityId = newSelectedCity?.id
//        sharedPrefUtil.selectedCity = newSelectedCity
//
//        if (sendBroadcast) {
//            localBroadcastManager
//                .sendBroadcast(
//                    Intent(ACTION_CHANGED_LOCATION).apply {
//                        putExtra(EXTRA_SELECTED_CITY, newSelectedCity)
//                        putExtra(SELF, true)
//                    }
//                )
//        }
    }

    override fun onStart() {
        super.onStart()

        repository.getAllCityAndCurrentWeathers()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { toast(it.message.toString()); debug(it, "@@@") },
                onNext = {
                    cityAdapter.submitList(it.also { weathers = it })
                }
            )

//        search_view.textChange()
//            .startWith("")
//            .debounce(500, TimeUnit.MILLISECONDS)
//            .distinctUntilChanged()
//            .map { it.toLowerCase() }
//            .combineLatest(
//                weatherRepository.getAllWeathers()
//                    .subscribeOn(Schedulers.io())
//            )
//            .map { (searchString, weathers) ->
//                debug("Location: '$searchString' ${weathers.size}", "@@@")
//                if (searchString.isBlank()) {
//                    weathers
//                } else {
//                    weathers.filter {
//                        searchString in it.city.name.toLowerCase()
//                            || searchString in it.city.country.toLowerCase()
//                            || searchString in it.description.toLowerCase()
//                            || searchString in it.main.toLowerCase()
//                    }
//                }
//            }
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribeBy(
//                onError = {},
//                onNext = {
//                    debug("Location: ${it.size}", "@@@")
//                    cityAdapter.submitList(it)
//                    weathers = it
//                }
//            )
//            .addTo(compositeDisposable)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
//        localBroadcastManager.unregisterReceiver(broadcastReceiver)
        compositeDisposeble1.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> true.also { finish() }
            else -> return super.onOptionsItemSelected(item)

        }
    }

//    inner class LocationActivityBroadcastReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            when (intent?.action) {
//                ACTION_CHANGED_LOCATION -> {
//                    if (!intent.getBooleanExtra(SELF, false)) {
//                        onChangeSelectedCity(intent.getParcelableExtra(EXTRA_SELECTED_CITY), false)
//                    }
//                }
//            }
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_location, menu)
        menu?.findItem(R.id.action_search)?.let(search_view::setMenuItem)
        return true
    }

    override fun onBackPressed() {
        if (search_view.isSearchOpen) {
            search_view.closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val ACTION_UPDATE_CURRENT_WEATHER = "ACTION_UPDATE_CURRENT_WEATHER"
        const val EXTRA_CURRENT_WEATHER = "EXTRA_CURRENT_WEATHER"

        const val ACTION_UPDATE_DAILY_WEATHERS = "ACTION_UPDATE_DAILY_WEATHERS"
        const val EXTRA_DAILY_WEATHERS = "EXTRA_DAILY_WEATHERS"

        const val SELF = "SELF"
    }
}