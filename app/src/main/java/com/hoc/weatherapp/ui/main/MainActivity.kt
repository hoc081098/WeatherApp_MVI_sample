package com.hoc.weatherapp.ui.main

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.ui.cities.CitiesActivity
import com.hoc.weatherapp.ui.main.chart.ChartFragment
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherFragment
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherFragment
import com.hoc.weatherapp.ui.map.MapActivity
import com.hoc.weatherapp.ui.setting.SettingsActivity
import com.hoc.weatherapp.utils.blur.GlideBlurTransformation
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.startActivity
import com.hoc.weatherapp.utils.ui.ZoomOutPageTransformer
import com.hoc.weatherapp.utils.ui.getBackgroundDrawableFromWeather
import com.hoc.weatherapp.utils.ui.getSoundUriFromCurrentWeather
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.get
import java.lang.ref.WeakReference

class MainActivity : MviActivity<MainContract.View, MainPresenter>(), MainContract.View {
  private val vibrantColorSubject = PublishSubject.create<Int>()
  private var mediaPlayer: MediaPlayer? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(FLAG_TRANSLUCENT_STATUS)

    setContentView(R.layout.activity_main)

    setSupportActionBar(toolbar)
    supportActionBar?.run {
      setDisplayShowTitleEnabled(false)
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(R.drawable.ic_playlist_add_white_24dp)
    }

    setupViewPager()

    if (savedInstanceState === null) {
      vibrantColorSubject.onNext(R.color.colorPrimaryDark)
    }
  }

  override fun onStop() {
    super.onStop()

    stopSound()
  }

  override fun onDestroy() {
    super.onDestroy()

    vibrantColorSubject.onComplete()

    stopSound()

    // free memory
    mediaPlayer?.release()
    mediaPlayer = null
  }

  private fun setupViewPager() {
    view_pager.run {
      val fragments: List<Fragment> = listOf(
        CurrentWeatherFragment(),
        DailyWeatherFragment(),
        ChartFragment()
      )
      adapter = SectionsPagerAdapter(
        supportFragmentManager,
        fragments
      )
      offscreenPageLimit = fragments.size

      setPageTransformer(true, ZoomOutPageTransformer())

      dots_indicator.setViewPager(view_pager)
      dots_indicator.setDotsClickable(true)
    }
  }

  private fun enableIndicatorAndViewPager(isEnable: Boolean) {
    if (isEnable) {
      dots_indicator.visibility = View.VISIBLE
      view_pager.pagingEnable = true
    } else {
      dots_indicator.visibility = View.INVISIBLE
      view_pager.setCurrentItem(0, true)
      view_pager.pagingEnable = false
    }
  }

  private class SectionsPagerAdapter(fm: FragmentManager, private val fragments: List<Fragment>) :
    FragmentPagerAdapter(fm) {
    override fun getItem(position: Int) = fragments[position]
    override fun getCount() = fragments.size
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      android.R.id.home -> true.also { startActivity<CitiesActivity>() }
      R.id.action_settings -> true.also { startActivity<SettingsActivity>() }
      R.id.action_map -> true.also { startActivity<MapActivity>() }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun updateBackground(
    weather: CurrentWeather,
    city: City
  ) {
    Glide
      .with(this)
      .asBitmap()
      .load(getBackgroundDrawableFromWeather(weather, city))
      .apply(
        RequestOptions
          .bitmapTransform(GlideBlurTransformation(this, 25f))
          .fitCenter()
          .centerCrop()
      )
      .transition(BitmapTransitionOptions.withCrossFade())
      .into(object : CustomViewTarget<ImageView, Bitmap>(image_background) {
        override fun onLoadFailed(errorDrawable: Drawable?) = Unit

        override fun onResourceCleared(placeholder: Drawable?) = Unit

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
          view.setImageBitmap(resource)
          getVibrantColor(resource, WeakReference(this@MainActivity))
        }
      })
  }


  private fun stopSound() {
    runCatching {
      mediaPlayer?.takeIf { it.isPlaying }?.stop()
    }
  }

  private fun playSound(weather: CurrentWeather) {
    runCatching {
      mediaPlayer?.takeIf { it.isPlaying }?.stop()
    }
    mediaPlayer =
      MediaPlayer.create(this, getSoundUriFromCurrentWeather(weather))
        .apply {
          setVolume(0.3f, 0.3f)
          runCatching { start() }.onSuccess { debug("MediaPlayer::start", "__main__") }
        }
  }

  override fun render(state: MainContract.ViewState) {
    when (state) {
      MainContract.ViewState.NoSelectedCity -> renderNoSelectedCity()
      is MainContract.ViewState.CityAndWeather -> renderCityAndWeather(state)
    }
  }

  override fun changeVibrantColorIntent() = vibrantColorSubject

  private fun renderCityAndWeather(state: MainContract.ViewState.CityAndWeather) {
    updateBackground(state.weather, state.city)
    toolbar_title.text = getString(
      R.string.city_name_and_country,
      state.city.name,
      state.city.country
    )
    playSound(state.weather)
    enableIndicatorAndViewPager(true)
  }

  private fun renderNoSelectedCity() {
    Glide.with(this)
      .load(R.drawable.default_bg)
      .transition(DrawableTransitionOptions.withCrossFade())
      .apply(RequestOptions.fitCenterTransform().centerCrop())
      .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
      .into(image_background)

    window.statusBarColor = ContextCompat.getColor(
      this@MainActivity,
      R.color.colorPrimaryDark
    )
    toolbar_title.text = getString(R.string.no_selected_city)
    stopSound()
    enableIndicatorAndViewPager(false)
  }

  override fun createPresenter() = get<MainPresenter>()

  companion object {
    private fun getVibrantColor(resource: Bitmap, mainActivity: WeakReference<MainActivity>) {
      Palette
        .from(resource)
        .generate { palette ->
          palette ?: return@generate

          val vibrantColor = listOf(
            palette.getSwatchForTarget(Target.DARK_VIBRANT)?.rgb,
            palette.getSwatchForTarget(Target.VIBRANT)?.rgb,
            palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb
          ).find { it != null }
            ?: mainActivity.get()?.let {
              ContextCompat.getColor(
                it,
                R.color.colorPrimaryDark
              )
            }
            ?: return@generate

          mainActivity.get()?.vibrantColorSubject?.onNext(vibrantColor)
          mainActivity.get()?.window?.statusBarColor = vibrantColor
        }
    }
  }
}
