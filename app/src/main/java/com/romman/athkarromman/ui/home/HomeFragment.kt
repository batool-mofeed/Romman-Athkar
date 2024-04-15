package com.romman.athkarromman.ui.home

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        notificationPermission()
    }


    private fun notificationPermission() {
        if (Build.VERSION.SDK_INT >= 32) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_NOTIFICATION_POLICY
                ) == PackageManager.PERMISSION_GRANTED
            ) return
            pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
        }
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
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToEditPrayersFragment(
                        viewModel?.prayerTimess!!
                    )
                )
            }
            athkarLayout.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAthkarFragment())
            }
            fajrLayout.setOnClickListener {
                showRemainingTimeForPrayer(viewModel?.prayerTimess?.get(0))
            }
            shuruqLayout.setOnClickListener {
                showRemainingTimeForPrayer(viewModel?.prayerTimess?.get(1))
            }
            duhurLayout.setOnClickListener {
                showRemainingTimeForPrayer(viewModel?.prayerTimess?.get(2))
            }
            asrLayout.setOnClickListener {
                showRemainingTimeForPrayer(viewModel?.prayerTimess?.get(3))
            }
            maghrebLayout.setOnClickListener {
                showRemainingTimeForPrayer(viewModel?.prayerTimess?.get(4))
            }
            ishaLayout.setOnClickListener {
                showRemainingTimeForPrayer(viewModel?.prayerTimess?.get(5))
            }

        }
    }

    private fun showRemainingTimeForPrayer(prayerTime: String?) {
        prayerTime?.let {
            // Parse the prayer time to get the hour and minute
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val prayerDateTime = formatter.parse(prayerTime)

            prayerDateTime?.let { dateTime ->
                // Get the current date and time
                val currentTime = Calendar.getInstance().time

                // Convert the prayer date to current year, month, and day
                val calendar = Calendar.getInstance()
                calendar.time = currentTime
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

                calendar.time = dateTime
                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.MONTH, currentMonth)
                calendar.set(Calendar.DAY_OF_MONTH, currentDay)

                val prayerDateTimeAdjusted = calendar.time

                // Calculate the difference in milliseconds between current time and prayer time
                val differenceMillis = prayerDateTimeAdjusted.time - currentTime.time

                // Calculate the remaining hours and minutes
                val remainingHours = TimeUnit.MILLISECONDS.toHours(differenceMillis)
                val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(differenceMillis) % 60

                // Create a toast to display the remaining time
                val remainingTimeText =
                    "Remaining time for prayer: $remainingHours hours and $remainingMinutes minutes"
                Toast.makeText(requireContext(), remainingTimeText, Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(requireContext(), "Invalid prayer time format", Toast.LENGTH_SHORT)
                    .show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Prayer time is not available", Toast.LENGTH_SHORT)
                .show()
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