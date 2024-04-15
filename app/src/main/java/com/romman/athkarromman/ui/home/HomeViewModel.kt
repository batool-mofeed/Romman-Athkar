package com.romman.athkarromman.ui.home

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romman.athkarromman.BuildConfig
import com.romman.athkarromman.data.remote.ApiClient
import com.romman.athkarromman.data.remote.ApisResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class HomeViewModel : ViewModel() {

    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    val cityName = MutableStateFlow("No location detected yet!")
    val prayerName = MutableStateFlow("")
    val prayerTime = MutableStateFlow("")
    val nextPrayerName = MutableStateFlow("")
    val nextPrayerTime = MutableStateFlow("")

    lateinit var prayerTimess: Array<String>

    fun loadPrayers(cityFile: String) {
        cityName.value = cityFile
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ApiClient.apiService.getPrayers(cityFile)
                if (response.isSuccessful) {
                    decryptResponse(response.body())
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    private fun handleException(e: Exception) {
        e.printStackTrace()
        _errorMessage.value = e.message.toString()
        _errorMessage.value = ""
        Timber.e("${e.message}")
        _loading.value = false
    }

    private fun decryptResponse(body: ApisResponse?) {
        body?.let {
            val decryptedData = decrypt(it.data)?.substring(16)
            println("Decrypted Data: $decryptedData")

            // Check if decryptedData is not null and has valid format
            if (!decryptedData.isNullOrEmpty()) {
                val map = parseData(decryptedData)

                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val today = LocalDate.now().format(formatter)

                val todayPrayerTimes = map[today]
                if (todayPrayerTimes != null) {
                    setPrayerTimes(todayPrayerTimes)
                } else {
                    println("No prayer times found for today.")
                }
            } else {
                println("Decrypted data is null or empty.")
            }

            _loading.value = false
        }
    }

    private fun parseData(decryptedData: String): Map<String, Array<String>> {
        val map = mutableMapOf<String, Array<String>>()

        val jsonObject = JSONObject(decryptedData.trim())
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val timesArray = jsonObject.getJSONArray(key)
            val times = Array(timesArray.length()) { index ->
                timesArray.getString(index)
            }
            println("Date: $key")
            println("Times: ${times.joinToString(", ")}")
            map[key] = times
        }

        return map
    }


    private fun setPrayerTimes(prayerTimes: Array<String>) {
        prayerTimess = prayerTimes
        val formatter = DateTimeFormatter.ofPattern("hh:mm a") // Define custom time formatter
        val currentTime = LocalTime.now()
        val nextPrayerIndex =
            prayerTimes.indexOfFirst { LocalTime.parse(it, formatter) > currentTime }
                .takeIf { it != -1 }
                ?: 0

        val prayerNames = listOf("Fajr", "Shuruq", "Dhuhr", "Asr", "Maghrib", "Isha")

        with(prayerTimes) {
            prayerTime.value = getOrNull(nextPrayerIndex) ?: firstOrNull() ?: ""
            prayerName.value = prayerNames.getOrElse(nextPrayerIndex) { "Unknown" }
            nextPrayerTime.value = getOrNull(nextPrayerIndex + 1) ?: firstOrNull() ?: ""
            nextPrayerName.value = prayerNames.getOrElse(nextPrayerIndex + 1) { "Unknown" }
        }
        println("vvvvvvvvvvvvvvvv ${prayerTime.value}")
    }


    private fun JSONObject.toMap(): Map<String, Array<String>> {
        val map = mutableMapOf<String, Array<String>>()
        for (key in keys()) {
            val value = optJSONArray(key)
            val stringArray = mutableListOf<String>()
            if (value != null) {
                for (i in 0 until value.length()) {
                    stringArray.add(value.optString(i))
                }
                map[key] = stringArray.toTypedArray()
            }
        }
        return map
    }

    private fun decrypt(text: String?): String? {
        text ?: return null

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val secretKey = SecretKeySpec(
            hashToSHA256(BuildConfig.SECRET_KEY).substring(0, 32).toByteArray(),
            "AES"
        )
        val ivParameterSpec = IvParameterSpec(generateIV())

        val encryptedByteArray: ByteArray = Base64.decode(text, Base64.DEFAULT)
        val encrypted = encryptedByteArray.copyOfRange(0, encryptedByteArray.size)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return try {
            String(cipher.doFinal(encrypted))
        } catch (e: Throwable) {
            null
        }
    }

    private fun generateIV(): ByteArray {
        return ByteArray(16)
    }

    private fun hashToSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
