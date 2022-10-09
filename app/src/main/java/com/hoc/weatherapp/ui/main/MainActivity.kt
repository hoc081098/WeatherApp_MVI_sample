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
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
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
import com.hoc.weatherapp.R
import com.hoc.weatherapp.data.models.entity.City
import com.hoc.weatherapp.data.models.entity.CurrentWeather
import com.hoc.weatherapp.databinding.ActivityMainBinding
import com.hoc.weatherapp.ui.BaseMviActivity
import com.hoc.weatherapp.ui.cities.CitiesActivity
import com.hoc.weatherapp.ui.main.chart.ChartFragment
import com.hoc.weatherapp.ui.main.currentweather.CurrentWeatherFragment
import com.hoc.weatherapp.ui.main.fivedayforecast.DailyWeatherFragment
import com.hoc.weatherapp.ui.map.MapActivity
import com.hoc.weatherapp.ui.setting.SettingsActivity
import com.hoc.weatherapp.utils.None
import com.hoc.weatherapp.utils.Optional
import com.hoc.weatherapp.utils.Some
import com.hoc.weatherapp.utils.blur.GlideBlurTransformation
import com.hoc.weatherapp.utils.debug
import com.hoc.weatherapp.utils.startActivity
import com.hoc.weatherapp.utils.themeColor
import com.hoc.weatherapp.utils.ui.ZoomOutPageTransformer
import com.hoc.weatherapp.utils.ui.getBackgroundDrawableFromWeather
import com.hoc.weatherapp.utils.ui.getSoundUriFromCurrentWeather
import com.hoc081098.viewbindingdelegate.viewBinding
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.get
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityRetainedScope
import org.koin.core.scope.Scope

@ExperimentalStdlibApi
class MainActivity :
  BaseMviActivity<MainContract.View, MainPresenter>(
    contentLayoutId = R.layout.activity_main
  ),
  MainContract.View,
  AndroidScopeComponent {
  private val binding by viewBinding<ActivityMainBinding>()

  private var mediaPlayer: MediaPlayer? = null
  private val changeBackground = PublishSubject.create<Optional<Bitmap>>()

  private var target1: CustomViewTarget<*, *>? = null
  private var target2: CustomViewTarget<*, *>? = null

  override var scope: Scope? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    createActivityRetainedScope()

    super.onCreate(savedInstanceState)

    window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(FLAG_TRANSLUCENT_STATUS)

    setSupportActionBar(binding.toolbar)
    supportActionBar?.run {
      setDisplayShowTitleEnabled(false)
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(R.drawable.ic_playlist_add_white_24dp)
    }

    setupViewPager()
  }

  override fun onStop() {
    super.onStop()

    stopSound()
  }

  override fun onDestroy() {
    super.onDestroy()

    stopSound()

    // free memory
    mediaPlayer?.release()
    mediaPlayer = null
  }

  private fun setupViewPager() {
    binding.viewPager.run {
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
    }

    binding.dotsIndicator.run {
      setViewPager(binding.viewPager)
      dotsClickable = true
    }
  }

  private fun enableIndicatorAndViewPager(isEnable: Boolean) {
    if (isEnable) {
      binding.dotsIndicator.visibility = View.VISIBLE
      binding.viewPager.pagingEnable = true
    } else {
      binding.dotsIndicator.visibility = View.INVISIBLE
      binding.viewPager.setCurrentItem(0, true)
      binding.viewPager.pagingEnable = false
    }
  }

  private class SectionsPagerAdapter(
    fm: FragmentManager,
    private val fragments: List<Fragment>
  ) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int) = fragments[position]
    override fun getCount() = fragments.size
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
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
      .apply {
        clear(target1)
        clear(target2)
        changeBackground.onNext(None)
      }
      .asBitmap()
      .load(getBackgroundDrawableFromWeather(weather, city))
      .apply(
        RequestOptions
          .bitmapTransform(GlideBlurTransformation(this, 20f))
          .fitCenter()
          .centerCrop()
      )
      .transition(BitmapTransitionOptions.withCrossFade())
      .into(object : CustomViewTarget<ImageView, Bitmap>(binding.imageBackground) {
        override fun onLoadFailed(errorDrawable: Drawable?) = Unit

        override fun onResourceCleared(placeholder: Drawable?) = Unit

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
          view.setImageBitmap(resource)
          changeBackground.onNext(Some(resource))
        }
      })
      .also { target1 = it }
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
    window.statusBarColor = state.vibrantColor
    when (state) {
      is MainContract.ViewState.NoSelectedCity -> renderNoSelectedCity()
      is MainContract.ViewState.CityAndWeather -> renderCityAndWeather(state)
    }
  }

  override fun changeColorIntent(): Observable<Pair<Int, Int>> {
    return changeBackground
      .switchMap { optional ->
        when (optional) {
          is Some -> {
            Observable
              .fromCallable {
                getVibrantColor(
                  resource = optional.value,
                  colorPrimaryVariant = themeColor(R.attr.colorPrimaryVariant),
                  colorSecondary = themeColor(R.attr.colorSecondary),
                )
              }
              .subscribeOn(Schedulers.computation())
          }
          None -> Observable.empty()
        }
      }
  }

  private fun renderCityAndWeather(state: MainContract.ViewState.CityAndWeather) {
    updateBackground(state.weather, state.city)

    binding.toolbarTitle.text = getString(
      R.string.city_name_and_country,
      state.city.name,
      state.city.country
    )
    playSound(state.weather)
    enableIndicatorAndViewPager(true)
  }

  private fun renderNoSelectedCity() {
    Glide.with(this)
      .apply {
        clear(target1)
        clear(target2)
        changeBackground.onNext(None)
      }
      .load(R.drawable.default_bg)
      .transition(DrawableTransitionOptions.withCrossFade())
      .apply(RequestOptions.fitCenterTransform().centerCrop())
      .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
      .into(object : CustomViewTarget<ImageView, Drawable>(binding.imageBackground) {
        override fun onLoadFailed(errorDrawable: Drawable?) = Unit
        override fun onResourceCleared(placeholder: Drawable?) = Unit
        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
          view.setImageDrawable(resource)
        }
      })
      .also { target2 = it }

    binding.toolbarTitle.text = getString(R.string.no_selected_city)
    stopSound()
    enableIndicatorAndViewPager(false)
  }

  override fun createPresenter() = get<MainPresenter>()
}

@WorkerThread
private fun getVibrantColor(
  resource: Bitmap,
  @ColorInt colorPrimaryVariant: Int,
  @ColorInt colorSecondary: Int,
): Pair<Int, Int> {
  return Palette
    .from(resource)
    .generate()
    .let { palette ->
      @ColorInt val darkColor = listOf(
        palette.getSwatchForTarget(Target.DARK_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb,
        palette.getSwatchForTarget(Target.MUTED)?.rgb,
        palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb
      ).find { it !== null } ?: colorPrimaryVariant

      @ColorInt val lightColor = listOf(
        palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb,
        palette.getSwatchForTarget(Target.LIGHT_MUTED)?.rgb
      ).find { it !== null } ?: colorSecondary

      darkColor to lightColor
    }
}
