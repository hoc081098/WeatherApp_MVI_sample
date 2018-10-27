package com.hoc.weatherapp.koin

import com.hoc.weatherapp.BuildConfig
import com.hoc.weatherapp.data.models.TemperatureUnit
import com.hoc.weatherapp.data.remote.APP_ID
import com.hoc.weatherapp.data.remote.BASE_URL
import com.hoc.weatherapp.data.remote.BASE_URL_HELPER
import com.hoc.weatherapp.data.remote.HelperApiService
import com.hoc.weatherapp.data.remote.WeatherApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

const val WEATHER_RETROFIT = "WEATHER_RETROFIT"
const val HELPER_RETROFIT = "HELPER_RETROFIT"

val retrofitModule = module {
  single { getOkHttpClient() }

  single { getMoshi() }

  single(name = WEATHER_RETROFIT) { getWeatherRetrofit() }

  single { getWeatherApiService() }

  single(name = HELPER_RETROFIT) { getHelperRetrofit() }

  single { getHelperApiService() }
}

private fun ModuleDefinition.getHelperApiService(): HelperApiService {
  return get<Retrofit>(name = HELPER_RETROFIT).create(
    HelperApiService::class.java
  )
}

private fun ModuleDefinition.getHelperRetrofit(): Retrofit {
  return Retrofit.Builder()
    .baseUrl(BASE_URL_HELPER)
    .client(get())
    .addConverterFactory(MoshiConverterFactory.create(get()))
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .build()
}

private fun ModuleDefinition.getWeatherApiService(): WeatherApiService {
  return get<Retrofit>(name = WEATHER_RETROFIT).create(
    WeatherApiService::class.java
  )
}

private fun ModuleDefinition.getWeatherRetrofit(): Retrofit {
  return Retrofit.Builder()
    .baseUrl(BASE_URL)
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