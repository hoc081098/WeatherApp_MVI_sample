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
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

const val OPEN_WEATHER_MAP_RETROFIT = "OPEN_WEATHER_MAP_RETROFIT"
const val TIMEZONE_DB_RETROFIT = "TIMEZONE_DB_RETROFIT"

val retrofitModule = module {
  single { getOkHttpClient() }

  single { getMoshi() }

  single(name = OPEN_WEATHER_MAP_RETROFIT) { getOpenWeatherMapRetrofit() }

  single { getWeatherApiService() }

  single(name = TIMEZONE_DB_RETROFIT) { getTimezoneDbRetrofit() }

  single { getTimezoneDbApiService() }
}

private fun ModuleDefinition.getTimezoneDbApiService(): TimezoneDbApiService {
  return get<Retrofit>(name = TIMEZONE_DB_RETROFIT).create(TimezoneDbApiService::class.java)
}

private fun ModuleDefinition.getTimezoneDbRetrofit(): Retrofit {
  return Retrofit.Builder()
    .baseUrl(BASE_URL_TIMEZONE_DB)
    .client(get())
    .addConverterFactory(MoshiConverterFactory.create(get()))
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .build()
}

private fun ModuleDefinition.getWeatherApiService(): OpenWeatherMapApiService {
  return get<Retrofit>(name = OPEN_WEATHER_MAP_RETROFIT)
    .create(OpenWeatherMapApiService::class.java)
}

private fun ModuleDefinition.getOpenWeatherMapRetrofit(): Retrofit {
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
      val host = originalRequest.url().host()

      when {
        "openweathermap" in host -> originalRequest
          .newBuilder()
          .url(
            originalRequest.url()
              .newBuilder()
              .addQueryParameter("units", TemperatureUnit.KELVIN.toString())
              .addQueryParameter("appid", OPEN_WEATHER_MAP_APP_ID)
              .build()
          )
        "timezonedb" in host -> {
          if ("get-time-zone" in originalRequest.url().encodedPath()) {
            originalRequest
              .newBuilder()
              .url(
                originalRequest.url()
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