package com.romman.athkarromman.ui.locationdialog

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romman.athkarromman.BuildConfig
import com.romman.athkarromman.data.model.City
import com.romman.athkarromman.data.remote.ApiClient
import com.romman.athkarromman.data.remote.ApisResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Created By Batool Mofeed - 08/04/2024.
 **/
class LocationDialogViewModel : ViewModel() {

    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities = _cities.asStateFlow()

    fun loadCities() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ApiClient.apiService.getCities()

                when {
                    response.isSuccessful -> {
                        println("Repsonseeee ${response.code()} ${response.body()}")
                        decryptResponse(response.body())
                    }
//                    response.code() in 500..550 -> {
//                    }
                }
            } catch (e: CancellationException) {
                e.printStackTrace()
                _errorMessage.value = e.message.toString()
                _errorMessage.value = ""
                Timber.e("${e.message}")
                _loading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.message.toString()
                _errorMessage.value = ""
                Timber.e("${e.message}")
                _loading.value = false
            }
        }
    }

    private fun decryptResponse(body: ApisResponse?) {

        body?.let {
            // Decrypt the data and remove the first 16 characters (IV block)
            val decryptedData = decrypt(it.data)?.substring(16)

            // Convert decrypted string to JSON object
            val jsonObject = decryptedData?.substring(
                decryptedData.indexOf("{"),
                decryptedData.lastIndexOf("}") + 1
            )
                ?.let { it1 -> JSONObject(it1) }

            // Extract the JSON array corresponding to "athkar"
            val citiesArray = jsonObject?.getJSONArray("cities")

            val moshi = Moshi.Builder().build()
            val cityListType = Types.newParameterizedType(List::class.java, City::class.java)
            // lenient for malformed server response
            val listAdapter = moshi.adapter<List<City>>(cityListType).lenient()
            val cityList = listAdapter.fromJson(citiesArray.toString())
            cityList?.let { cities ->
                _cities.value = cities
            }

            _loading.value = false
        }
    }

    fun generateIV(): ByteArray {
        return ByteArray(16)
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
            e.printStackTrace()
            null
        }
    }

    private fun hashToSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

}