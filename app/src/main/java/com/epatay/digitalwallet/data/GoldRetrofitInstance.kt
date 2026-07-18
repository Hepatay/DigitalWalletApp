package com.epatay.digitalwallet.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoldRetrofitInstance {

    private const val BASE_URL =
        "https://api.gold-api.com/"

    val api: GoldApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(GoldApiService::class.java)
    }
}