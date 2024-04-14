package com.romman.athkarromman.ui.athkar

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romman.athkarromman.BuildConfig
import com.romman.athkarromman.data.model.AthkarItem
import com.romman.athkarromman.data.model.City
import com.romman.athkarromman.data.remote.ApiClient
import com.romman.athkarromman.data.remote.ApisResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AthkarViewModel : ViewModel() {

    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _athkar = MutableStateFlow<List<AthkarItem>>(emptyList())
    val athkar = _athkar.asStateFlow()

    fun loadAthkar() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ApiClient.apiService.getAthkar()
                if (response.isSuccessful) {
                    println("Response athkarr :: ${response.code()} ${response.body()}")
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
            // Decrypt the data and remove the first 16 characters (IV block)
            val decryptedData = decrypt(it.data)?.substring(16)

            // Convert decrypted string to JSON object
            val jsonObject = decryptedData?.substring(
                decryptedData.indexOf("{"),
                decryptedData.lastIndexOf("}") + 1
            )
                ?.let { it1 -> JSONObject(it1) }

            // Extract the JSON array corresponding to "athkar"
            val athkarsArray = jsonObject?.getJSONArray("athkars")

            val moshi = Moshi.Builder().build()
            val athkarListType =
                Types.newParameterizedType(List::class.java, AthkarItem::class.java)
            // lenient for malformed server response
            val listAdapter = moshi.adapter<List<AthkarItem>>(athkarListType).lenient()
            val athkarsList = listAdapter.fromJson(athkarsArray.toString())
            athkarsList?.let { aa ->
                _athkar.value = aa
            }
            _loading.value = false
        }
    }


    fun generateIV(): ByteArray {
        val iv = ByteArray(16)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(iv)
        return iv
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