package com.hoc.weatherapp.koin

import com.hoc.weatherapp.BuildConfig
import com.hoc.weatherapp.data.models.apiresponse.TemperatureUnit
import com.hoc.weatherapp.data.remote.*
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
const val HELPER_RETROFIT = "HELPER_RETROFIT"

val retrofitModule = module {
  single { getOkHttpClient() }

  single { getMoshi() }

  single(name = OPEN_WEATHER_MAP_RETROFIT) { getOpenWeatherMapRetrofit() }

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

private fun ModuleDefinition.getWeatherApiService(): OpenWeatherMapApiService {
  return get<Retrofit>(name = OPEN_WEATHER_MAP_RETROFIT).create(
    OpenWeatherMapApiService::class.java
  )
}

private fun ModuleDefinition.getOpenWeatherMapRetrofit(): Retrofit {
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