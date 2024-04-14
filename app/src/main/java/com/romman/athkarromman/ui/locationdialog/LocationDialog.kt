package com.romman.athkarromman.ui.locationdialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.romman.athkarromman.R
import com.romman.athkarromman.databinding.DialogLocationBinding
import com.romman.athkarromman.utils.LocationHelper
import com.romman.athkarromman.utils.buildProgressDialog
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * Created By Batool Mofeed - 08/04/2024.
 **/
class LocationDialog : DialogFragment() {

    companion object {
        fun newInstance(doneFile: (String) -> Unit): LocationDialog {
            val fragment = LocationDialog()
            fragment.doneFileCallback = doneFile
            return fragment
        }
    }

    // Define a variable to hold the callback function
    private var doneFileCallback: ((String) -> Unit)? = null

    private var viewmodel: LocationDialogViewModel? = null
    lateinit var binding: DialogLocationBinding

    private lateinit var citiesAdapter: CityDropDownAdapter
    private var progressDialog: Dialog? = null

    private val locationHelper by lazy {
        LocationHelper(requireContext())
    }

    var cityFile = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        setStyle(STYLE_NO_TITLE, R.style.DialogStyle)
        val locationDialogViewModel by viewModels<LocationDialogViewModel>()
        viewmodel = locationDialogViewModel
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_location, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        initClicks()
        initCitiesRecycler()
        viewmodel?.loadCities()
    }

    private fun initClicks() {
        with(binding) {
            autoBtn.setOnClickListener {
                locationHelper.loadUserLocation { latLng, address ->
                    println("Adddreessss  ${address}")
                    if (address.isNotEmpty()) {
                        doneFileCallback?.let { it1 -> it1(address) }
                        dismiss()
                    }
                }
            }
            categoryText.setOnClickListener {
                citiesRecycler.isVisible = !citiesRecycler.isVisible
            }
            doneBtn.setOnClickListener {
                if (cityFile.isNotEmpty()) {
                    doneFileCallback?.let { it1 -> it1(cityFile) }
                    dismiss()
                }
            }
        }
    }

    private fun observeViewModel() {
        with(viewmodel) {
            this?.errorMessage?.collectFlow {
                if (it.isNotEmpty()) {
                    toast(it)
                }
            }
            this?.loading?.collectFlow {
                showProgressDialog(it)
            }
            this?.cities?.collectFlow {
                if (it.isNotEmpty()) {
                    citiesAdapter.addItems(it)
                }
            }
        }
    }

    fun showProgressDialog(show: Boolean) {
        if (show) {
            showProgressDialog()
        } else {
            hideProgressDialog()
        }
    }

    fun showProgressDialog() {
        try {
            hideProgressDialog()
            progressDialog =
                requireContext().buildProgressDialog()
            progressDialog?.show()
        } catch (e: java.lang.Exception) {
            Timber.e("${e.message}")
        }
    }

    fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    fun <T> StateFlow<T>.collectFlow(data: (T) -> Unit) {
        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                collect(data)
            }
        }
    }

    private fun toast(string: String) {
        try {
            Toast.makeText(requireContext(), string, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initCitiesRecycler() {
        binding.citiesRecycler.apply {
            layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            citiesAdapter = CityDropDownAdapter { city ->
                cityFile = city.file
                binding.categoryText.text = city.name_en
                binding.citiesRecycler.isVisible = false
            }
            adapter = citiesAdapter
        }

    }
}