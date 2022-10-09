package com.hoc.weatherapp.koin

import com.hoc.weatherapp.BuildConfig
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.remote.BASE_URL_TIMEZONE_DB
import com.hoc.weatherapp.data.remote.OPEN_WEATHER_MAP_APP_ID
import com.hoc.weatherapp.data.remote.OPEN_WEATHER_MAP_BASE_URL
import com.hoc.weatherapp.data.remote.OpenWeatherMapApiService
import com.hoc.weatherapp.data.remote.TIMEZONE_DB_API_KEY
import com.hoc.weatherapp.data.remote.TimezoneDbApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

private val OPEN_WEATHER_MAP_RETROFIT = named("OPEN_WEATHER_MAP_RETROFIT")
private val TIMEZONE_DB_RETROFIT = named("TIMEZONE_DB_RETROFIT")

val retrofitModule = module {
  single { getOkHttpClient() }

  single { getMoshi() }

  single(OPEN_WEATHER_MAP_RETROFIT) { getOpenWeatherMapRetrofit() }

  single { getWeatherApiService() }

  single(TIMEZONE_DB_RETROFIT) { getTimezoneDbRetrofit() }

  single { getTimezoneDbApiService() }
}

private fun Scope.getTimezoneDbApiService(): TimezoneDbApiService {
  return get<Retrofit>(TIMEZONE_DB_RETROFIT)
    .create(TimezoneDbApiService::class.java)
}

private fun Scope.getTimezoneDbRetrofit(): Retrofit {
  return Retrofit.Builder()
    .baseUrl(BASE_URL_TIMEZONE_DB)
    .client(get())
    .addConverterFactory(MoshiConverterFactory.create(get()))
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .build()
}

private fun Scope.getWeatherApiService(): OpenWeatherMapApiService {
  return get<Retrofit>(OPEN_WEATHER_MAP_RETROFIT)
    .create(OpenWeatherMapApiService::class.java)
}

private fun Scope.getOpenWeatherMapRetrofit(): Retrofit {
  return Retrofit.Builder()
    .baseUrl(OPEN_WEATHER_MAP_BASE_URL)
    .client(get())
    .addConverterFactory(MoshiConverterFactory.create(get()))
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .build()
}

private fun getMoshi(): Moshi {
  return Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
}

private fun getOkHttpClient(): OkHttpClient {
  return OkHttpClient.Builder()
    .apply {
      if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor()
          .setLevel(HttpLoggingInterceptor.Level.BODY)
          .let(::addInterceptor)
      }
    }
    .addInterceptor { chain ->
      val originalRequest = chain.request()
      val host = originalRequest.url.host

      when {
        "openweathermap" in host ->
          originalRequest
            .newBuilder()
            .url(
              originalRequest.url
                .newBuilder()
                .addQueryParameter("units", TemperatureUnit.KELVIN.toString())
                .addQueryParameter("appid", OPEN_WEATHER_MAP_APP_ID)
                .build()
            )
        "timezonedb" in host -> {
          if ("get-time-zone" in originalRequest.url.encodedPath) {
            originalRequest
              .newBuilder()
              .url(
                originalRequest.url
                  .newBuilder()
                  .addQueryParameter("format", "json")
                  .addQueryParameter("key", TIMEZONE_DB_API_KEY)
                  .addQueryParameter("by", "position")
                  .build()
              )
          } else {
            return@addInterceptor chain.proceed(originalRequest)
          }
        }
        else -> return@addInterceptor chain.proceed(originalRequest)
      }.build().let(chain::proceed)
    }
    .build()
}
