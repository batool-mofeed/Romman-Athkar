package com.romman.athkarromman.ui.editprayers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.romman.athkarromman.R
import com.romman.athkarromman.databinding.FragmentEditPrayersBinding
import com.romman.athkarromman.utils.PrayerNotificationReceiver
import com.romman.athkarromman.utils.Prefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditPrayersFragment : Fragment() {
    private var viewmodel: EditPrayersViewModel? = null
    lateinit var binding: FragmentEditPrayersBinding

    private val args by navArgs<EditPrayersFragmentArgs>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editPrayersViewModel by viewModels<EditPrayersViewModel>()
        viewmodel = editPrayersViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_edit_prayers, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSwitches()
        initView()
        initClicks()
    }

    private fun initView() {
        with(binding) {
            fajrAzanTxt.text = args.prayerTimes[0]
            duhurAzanTxt.text = args.prayerTimes[2]
            asrAzanTxt.text = args.prayerTimes[3]
            maghrebAzanTxt.text = args.prayerTimes[4]
            ishaAzanTxt.text = args.prayerTimes[5]
        }
    }

    private fun initSwitches() {
        with(binding) {
            fajrSwitch.isChecked = Prefs["Fajir", false]
            duhurSwitch.isChecked = Prefs["Duhur", false]
            asrSwitch.isChecked = Prefs["Asr", false]
            maghrebSwitch.isChecked = Prefs["Maghreb", false]
            ishaSwitch.isChecked = Prefs["Isha", false]
        }
    }

    private fun initClicks() {
        with(binding) {
            backBtn.setOnClickListener {
                findNavController().navigateUp()
            }
            saveBtn.setOnClickListener {
                Prefs["Fajir"] = fajrSwitch.isChecked
                Prefs["Duhur"] = duhurSwitch.isChecked
                Prefs["Asr"] = asrSwitch.isChecked
                Prefs["Maghreb"] = maghrebSwitch.isChecked
                Prefs["Isha"] = ishaSwitch.isChecked
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()

                scheduleNotifications()
            }
        }
    }

    private fun scheduleNotifications() {
        val prayerTimes = args.prayerTimes

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), PrayerNotificationReceiver::class.java)

        val prayerSwitches = mapOf(
            0 to binding.fajrSwitch.isChecked,
            2 to binding.duhurSwitch.isChecked,
            3 to binding.asrSwitch.isChecked,
            4 to binding.maghrebSwitch.isChecked,
            5 to binding.ishaSwitch.isChecked
        )

        val calendar = Calendar.getInstance()

        for ((prayer, isSwitchOn) in prayerSwitches) {
            val timeString = prayerTimes[prayer]
            if (isSwitchOn && timeString != null) {
                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val date = formatter.parse(timeString)
                date?.let {
                    calendar.time = it

                    val pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        prayer.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.cancel(pendingIntent) // Cancel the existing alarm

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            } else {
                // If the switch is off, cancel the corresponding alarm
                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    prayer.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let {
                    alarmManager.cancel(it)
                    it.cancel()
                }
            }
        }
    }


}