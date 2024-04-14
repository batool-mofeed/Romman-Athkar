package com.romman.athkarromman.data.model

import com.squareup.moshi.JsonClass

/**
 * Created By Batool Mofeed - 11/04/2024.
 **/
@JsonClass(generateAdapter = true)
data class AthkarItem(val text: String, val link: String)
