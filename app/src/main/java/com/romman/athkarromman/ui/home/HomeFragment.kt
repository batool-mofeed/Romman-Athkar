package com.romman.athkarromman.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.romman.athkarromman.BR
import com.romman.athkarromman.R
import com.romman.athkarromman.databinding.FragmentHomeBinding
import com.romman.athkarromman.ui.locationdialog.LocationDialog
import com.romman.athkarromman.utils.Prefs
import com.romman.athkarromman.utils.buildProgressDialog
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeFragment : Fragment() {

    private var viewModel: HomeViewModel? = null
    lateinit var binding: FragmentHomeBinding
    private var progressDialog: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val homeViewModel by viewModels<HomeViewModel>()
        viewModel = homeViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        with(binding) {
            lifecycleOwner = viewLifecycleOwner
            setVariable(BR.viewmodel, viewModel)
            executePendingBindings()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkLocation()
        initClicks()
        observeViewModel()
    }

    private fun checkLocation() {
        if (Prefs["location", ""].isEmpty()) {
            LocationDialog.newInstance {
                viewModel?.loadPrayers(it)
                Prefs["location"] = it

            }.show(parentFragmentManager, null)
        } else {
            viewModel?.loadPrayers(Prefs["location", ""])
        }
    }

    private fun observeViewModel() {
        with(viewModel) {
            this?.loading?.collectFlow(::showProgressDialog)
            this?.errorMessage?.collectFlow {
                if (it.isNotEmpty()) {
                    toast(it)
                }
            }
        }
    }

    fun initClicks() {
        with(binding) {
            editBtn.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToEditPrayersFragment())
            }
            athkarLayout.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAthkarFragment())
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

}