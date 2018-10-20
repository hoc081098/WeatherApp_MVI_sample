package com.hoc.weatherapp.koin

import com.hoc.weatherapp.BuildConfig
import com.hoc.weatherapp.data.remote.*
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

const val WEATHER_RETROFIT = "WEATHER_RETROFIT"
const val HELPER_RETROFIT = "HELPER_RETROFIT"

val retrofitModule = module {
  single<OkHttpClient> {
    OkHttpClient.Builder()
      .apply {
        if (BuildConfig.DEBUG) {
          HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)
            .let(::addInterceptor)
        }
      }
      .addInterceptor { chain ->
        chain.request().let { originalRequest ->
          originalRequest
            .newBuilder()
            .url(
              originalRequest.url()
                .newBuilder()
                .addQueryParameter("units", TemperatureUnit.KELVIN.toString())
                .addQueryParameter("appid", APP_ID)
                .build()
            )
            .build()
            .let(chain::proceed)
        }
      }
      .build()
  }

  single<Moshi> {
    Moshi.Builder()
      .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()
  }

  single<Retrofit>(name = WEATHER_RETROFIT) {
    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(get())
      .addConverterFactory(MoshiConverterFactory.create(get()))
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .build()
  }


  single<WeatherApiService> {
    get<Retrofit>(name = WEATHER_RETROFIT).create(
      WeatherApiService::class.java
    )
  }

  single<Retrofit>(name = HELPER_RETROFIT) {
    Retrofit.Builder()
      .baseUrl(BASE_URL_HELPER)
      .client(get())
      .addConverterFactory(MoshiConverterFactory.create(get()))
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .build()
  }

  single<HelperApiService> {
    get<Retrofit>(name = HELPER_RETROFIT).create(
      HelperApiService::class.java
    )
  }
}