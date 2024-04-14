package com.romman.athkarromman.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/
@JsonClass(generateAdapter = true)
@Parcelize
data class City(
    val id: String,
    val file: String,
    val name_ar: String,
    val name_en: String,
    val location: Location
) : Parcelable {
    override fun toString(): String {
        return name_en
    }
}