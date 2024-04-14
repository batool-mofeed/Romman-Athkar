package com.romman.athkarromman.utils

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener


/**
 * Created By Batool Mofeed - 08/04/2024.
 **/
class PermissionsHelper(
    private val context: Context
) {

    fun checkLocationPermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit = {},
        dismiss: () -> Unit = {}
    ) {
        Dexter.withContext(context)
            .withPermission(ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    onPermissionGranted()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    onPermissionDenied()
                    response?.let {
                        if (it.isPermanentlyDenied) {
                            showSettingsDialog(dismiss)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    fun checkWriteStoragePermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit = {},
        dismiss: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            onPermissionGranted()
        } else {
            requestPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                onPermissionGranted,
                onPermissionDenied,
                dismiss
            )
        }
    }

    private fun requestPermission(
        permission: String,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit = {},
        dismiss: () -> Unit = {}
    ) {
        Dexter.withContext(context)
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    onPermissionGranted()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    onPermissionDenied()
                    response?.let {
                        if (it.isPermanentlyDenied) {
                            showSettingsDialog(dismiss)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    fun showSettingsDialog(
        dismiss: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context).apply {
            setTitle("Location Permission Required")
            setMessage("To use this feature, please grant the location permission")
            setPositiveButton("Go to settings") { dialog, _ ->
                dialog.cancel()
                openAppSettings()
                dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
                dismiss()
            }
        }.show()
    }


    private fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.packageName)
            )
        )
    }

}