package com.hoc.weatherapp.data

import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL_HELPER = "https://drink-shop.com"

interface HelperApiService {
    @GET
    fun getNearbyCityByCountryCode(
            @Query("country") countryCode: String
    ): Flowable<List<NearCity>>
}