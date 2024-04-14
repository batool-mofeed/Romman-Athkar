package com.romman.athkarromman.ui.editprayers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.romman.athkarromman.R
import com.romman.athkarromman.databinding.FragmentEditPrayersBinding
import com.romman.athkarromman.utils.Prefs

class EditPrayersFragment : Fragment() {
    private var viewmodel: EditPrayersViewModel? = null
    lateinit var binding: FragmentEditPrayersBinding


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
        initClicks()
    }

    private fun initSwitches() {
        with(binding) {
            fajrSwitch.isChecked = Prefs["Fajir", false]
            fajrSwitch.isChecked = Prefs["Duhur", false]
            fajrSwitch.isChecked = Prefs["Asr", false]
            fajrSwitch.isChecked = Prefs["Maghreb", false]
            fajrSwitch.isChecked = Prefs["Isha", false]
        }
    }

    private fun initClicks() {
        with(binding) {
            backBtn.setOnClickListener {

            }
            saveBtn.setOnClickListener {
                Prefs["Fajir"] = fajrSwitch.isChecked
                Prefs["Duhur"] = duhurSwitch.isChecked
                Prefs["Asr"] = asrSwitch.isChecked
                Prefs["Maghreb"] = maghrebSwitch.isChecked
                Prefs["Isha"] = ishaSwitch.isChecked
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
            }
        }
    }


}