package com.romman.athkarromman.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.romman.athkarromman.R

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/

fun Context.buildProgressDialog() = Dialog(this).apply {
    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    setContentView(R.layout.dialog_progress)
    //findViewById<TextView>(R.id.msg)?.text = message
    setCancelable(false)
}


fun BottomSheetDialogFragment.initBottomSheetBehavior(stateChanged: (Int) -> Unit) {
    // expand the bottom sheet
    (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    // Set the callback to know the state of the bottom sheet
    val sheetBehavior = (this.dialog as BottomSheetDialog).behavior
    sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            stateChanged.invoke(newState) // only the state needed in this use case
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    })
}
