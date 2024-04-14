package com.romman.athkarromman.ui.exportsheet

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.romman.athkarromman.R
import com.romman.athkarromman.databinding.SheetExportBinding
import com.romman.athkarromman.utils.initBottomSheetBehavior

/**
 * Created By Batool Mofeed - 09/04/2024.
 **/
class ExportSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(imgFile: () -> Unit, videoFile: () -> Unit): ExportSheet {
            val fragment = ExportSheet()
            fragment.imgCallback = imgFile
            fragment.videoCallback = videoFile
            return fragment
        }
    }

    private var imgCallback: (() -> Unit)? = null
    private var videoCallback: (() -> Unit)? = null


    lateinit var binding: SheetExportBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.sheet_export, container, false)
        initBottomSheetBehaviorListener()
        return binding.root
    }

    private fun initBottomSheetBehaviorListener() {
        initBottomSheetBehavior { state ->
            when (state) {
                BottomSheetBehavior.STATE_HIDDEN -> dismiss()
                BottomSheetBehavior.STATE_COLLAPSED -> dismiss()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        view.setBackgroundResource(R.drawable.bg_top_radius)
        initClicks()
    }

    private fun initClicks() {
        with(binding) {
            imgBtn.setOnClickListener {
                imgCallback?.let { it1 -> it1() }
                dismiss()
            }
            videoBtn.setOnClickListener {
                videoCallback?.let { it1 -> it1() }
                dismiss()
            }
        }
    }

}