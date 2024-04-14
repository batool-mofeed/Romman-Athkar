package com.romman.athkarromman.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/
interface ApiServices {
    @GET("athkar.json")
    suspend fun getCities(
    ): Response<ApisResponse>

    @GET("prayers/{fileName}.json")
    suspend fun getPrayers(
        @Path("fileName") fileName:String
    ): Response<ApisResponse>

    @GET("athkars.json")
    suspend fun getAthkar(
    ): Response<ApisResponse>

}