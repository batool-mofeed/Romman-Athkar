package com.romman.athkarromman.data.remote

import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/
object RetrofitClient {
    private const val BASE_URL = "https://rommanapps.com/android/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder()
                        .build()
                )
            )
            .build()
    }
}

object ApiClient {
    val apiService: ApiServices by lazy {
        RetrofitClient.retrofit.create(ApiServices::class.java)
    }
}