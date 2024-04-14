package com.romman.athkarromman.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/

@JsonClass(generateAdapter = true)
data class ApisResponse(
    @Json(name = "data")
    val data: String
)

