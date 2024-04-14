package com.romman.athkarromman.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.birjuvachhani.locus.Locus
import com.birjuvachhani.locus.extensions.isDenied
import com.birjuvachhani.locus.extensions.isFatal
import com.birjuvachhani.locus.extensions.isPermanentlyDenied
import com.birjuvachhani.locus.extensions.isSettingsDenied
import com.birjuvachhani.locus.extensions.isSettingsResolutionFailed
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/

class LocationHelper(
    private val context: Context
) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val permissionHelper by lazy {
        PermissionsHelper(context)
    }

    @SuppressLint("MissingPermission")
    fun loadUserLocation(
        result: (LatLng, String) -> Unit
    ) {
        permissionHelper.checkLocationPermission(
            onPermissionGranted = {
                fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val location = task.result
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            loadAddressFromCoordinates(
                                latLng.latitude,
                                latLng.longitude
                            ) { addressName ->
                                result(latLng, addressName)
                            }
                        } else {
                            loadCurrentLocation(result)
                        }
                    } else {
                        loadCurrentLocation(result)
                    }
                }
            }
        )
    }

    fun loadCurrentLocation(locationResult: (LatLng, String) -> Unit) {
        Locus.getCurrentLocation(context) { result ->
            result.location?.let {
                loadAddressFromCoordinates(it.latitude, it.longitude) { addressName ->
                    locationResult(
                        LatLng(it.latitude, it.longitude),
                        addressName
                    )
                }
            }
            result.error?.let { error ->
                when {
                    error.isDenied -> { /* Permission denied */
                    }

                    error.isPermanentlyDenied -> { /* Permission is permanently denied */
                    }

                    error.isFatal -> { /* Something else went wrong! */
                    }

                    error.isSettingsDenied -> { /* Settings resolution denied by the user */
                    }

                    error.isSettingsResolutionFailed -> { /* Settings resolution failed! */
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun loadAddressFromCoordinates(LATITUDE: Double, LONGITUDE: Double, address: (String) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            geocoder.getFromLocation(LATITUDE, LONGITUDE, 1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        addresses[0].getAddressLine(0)
                    }

                    override fun onError(errorMessage: String?) {
                        super.onError(errorMessage)

                    }

                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}