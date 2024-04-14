package com.romman.athkarromman.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/
@JsonClass(generateAdapter = true)
@Parcelize
data class Location(
    val lat: Double,
    val long: Double,
) : Parcelable