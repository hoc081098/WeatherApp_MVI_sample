package com.hoc.weatherapp.ui.cities

import android.graphics.Canvas
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.databinding.ActivityCitiesBinding
import com.hoc.weatherapp.ui.BaseMviActivity
import com.hoc.weatherapp.ui.addcity.AddCityActivity
import com.hoc.weatherapp.ui.cities.CitiesContract.SearchStringIntent
import com.hoc.weatherapp.ui.cities.CitiesContract.SearchStringIntent.InitialSearchStringIntent
import com.hoc.weatherapp.ui.cities.CitiesContract.View
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.snackBar
import com.hoc.weatherapp.utils.startActivity
import com.hoc.weatherapp.utils.ui.SwipeController
import com.hoc.weatherapp.utils.ui.SwipeControllerActions
import com.hoc.weatherapp.utils.ui.textChanges
import com.hoc081098.viewbindingdelegate.viewBinding
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import io.reactivex.subjects.PublishSubject.create
import java.util.concurrent.TimeUnit
import kotlin.LazyThreadSafetyMode.NONE
import org.koin.android.ext.android.get

@ExperimentalStdlibApi
class CitiesActivity : BaseMviActivity<View, CitiesPresenter>(R.layout.activity_cities), View {
  private val binding by viewBinding<ActivityCitiesBinding>()

  private var refreshSnackBar: Snackbar? = null
  private var snackBar: Snackbar? = null
  private var deleteSnackBar: Snackbar? = null

  private val cityAdapter = CitiesAdapter()
  private val root by lazy(NONE) { findViewById<android.view.View>(android.R.id.content)!! }

  private val deletePositionPublishSubject = create<Int>()
  private val refreshPositionPublishSubject = create<Int>()
  private val searchStringInitial = create<InitialSearchStringIntent>()

  override fun createPresenter() = get<CitiesPresenter>()

  override fun refreshCurrentWeatherAtPosition() =
    refreshPositionPublishSubject.hide()!!

  override fun deleteCityAtPosition() = deletePositionPublishSubject.hide()!!

  override fun changeSelectedCity(): Observable<City> {
    return cityAdapter
      .itemClickObservable
      .throttleFirst(500, TimeUnit.MILLISECONDS)
  }

  override fun searchStringIntent(): Observable<SearchStringIntent> {
    return binding.searchView.textChanges()
      .debounce(600, TimeUnit.MILLISECONDS)
      .map { SearchStringIntent.UserSearchStringIntent(it) }
      .cast<SearchStringIntent>()
      .mergeWith(searchStringInitial)
      .distinctUntilChanged()
      .doOnNext { debug("searchStringIntent '$it'") }
  }

  override fun render(state: CitiesContract.ViewState) {
    cityAdapter.submitList(state.cityListItems)

    if (state.error != null && state.showError) {
      snackBar?.dismiss()
      snackBar = root.snackBar(
        state.error.message ?: "An error occurred!",
        Snackbar.LENGTH_INDEFINITE
      )
    }
    if (!state.showError) {
      snackBar?.dismiss()
    }

    if (state.deletedCity != null && state.showDeleteCitySuccessfully) {
      deleteSnackBar?.dismiss()
      deleteSnackBar = root.snackBar(
        "Delete city ${state.deletedCity.name} successfully",
        Snackbar.LENGTH_INDEFINITE
      )
    }
    if (!state.showDeleteCitySuccessfully) {
      deleteSnackBar?.dismiss()
    }

    if (state.refreshCity != null && state.showRefreshSuccessfully) {
      refreshSnackBar?.dismiss()
      refreshSnackBar = root.snackBar(
        "Refresh weather of city ${state.refreshCity.name} successfully",
        Snackbar.LENGTH_INDEFINITE
      )
    }
    if (!state.showRefreshSuccessfully) {
      refreshSnackBar?.dismiss()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setSupportActionBar(binding.toolbar)
    supportActionBar?.run {
      setDisplayHomeAsUpEnabled(true)
      title = "City"
    }
    binding.fab.setOnClickListener { startActivity<AddCityActivity>() }

    binding.searchView.run {
      setHint("Search...")
      setHintTextColor(
        ContextCompat.getColor(this@CitiesActivity, R.color.colorPrimaryText)
      )
    }
    setupRecyclerViewCities()
  }

  override fun onStart() {
    super.onStart()
    searchStringInitial.onNext(InitialSearchStringIntent)
  }

  private fun setupRecyclerViewCities() {
    binding.recyclerCity.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(this@CitiesActivity)
      adapter = cityAdapter

      addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          super.onScrolled(recyclerView, dx, dy)
          if (dy > 0) {
            binding.fab.hide()
          } else {
            binding.fab.show()
          }
        }
      })

      val swipeController = SwipeController(object :
          SwipeControllerActions {
          override fun onLeftClicked(adapterPosition: Int) {
            refreshPositionPublishSubject.onNext(adapterPosition)
          }

          override fun onRightClicked(adapterPosition: Int) {
            AlertDialog.Builder(this@CitiesActivity)
              .setTitle("Delete city")
              .setMessage("Do you want to delete this city")
              .setIcon(R.drawable.ic_delete_black_24dp)
              .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
              .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                deletePositionPublishSubject.onNext(adapterPosition)
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

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> true.also { finish() }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_location, menu)
    menu?.findItem(R.id.action_search)?.let(binding.searchView::setMenuItem)
    return true
  }

  override fun onBackPressed() {
    if (binding.searchView.isSearchOpen) {
      binding.searchView.closeSearch()
    } else {
      super.onBackPressed()
    }
  }
}
