package com.romman.athkarromman.ui.athkar


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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.romman.athkarromman.R
import com.romman.athkarromman.data.model.AthkarItem
import com.romman.athkarromman.databinding.FragmentAthkarBinding
import com.romman.athkarromman.utils.buildProgressDialog
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AthkarFragment : Fragment() {

    private var viewmodel: AthkarViewModel? = null
    private lateinit var binding: FragmentAthkarBinding

    private var progressDialog: Dialog? = null

    private lateinit var athkarList: List<AthkarItem>

    private var currentIndex = 0
    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val athkarViewModel by viewModels<AthkarViewModel>()
        viewmodel = athkarViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_athkar, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClicks()
        observeViewModel()
        viewmodel?.loadAthkar()
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
            this?.athkar?.collectFlow {
                if (it.isNotEmpty()) {
                    athkarList = it
                    updateAthkar()
                }
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

    private fun initClicks() {
        with(binding) {
            athkarBackBtn.setOnClickListener {
                if (currentIndex > 0) {
                    currentIndex--
                    updateAthkar()
                    stopPlayback()
                }
            }
            athkarNextBtn.setOnClickListener {
                if (currentIndex < athkarList.size - 1) {
                    currentIndex++
                    updateAthkar()
                    stopPlayback()
                }
            }
            playPauseBtn.setOnClickListener {
                val currentAthkar = athkarList[currentIndex]
                if (isPlaying) {
                    stopPlayback()
                } else {
                    startPlayback(currentAthkar.link)
                }
            }
            exportBtn.setOnClickListener {
                findNavController().navigate(
                    AthkarFragmentDirections.actionAthkarFragmentToExportFragment2(
                        athkarList[currentIndex].text,
                        athkarList[currentIndex].link
                    )
                )
            }
            backBtn.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun updateAthkar() {
        val currentAthkar = athkarList[currentIndex]
        binding.athkarTxt.text = currentAthkar.text
        if (isPlaying) {
            stopPlayback()
        }
    }

    private fun startPlayback(audioUrl: String) {
        releasePlayer()
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        val mediaItem = MediaItem.fromUri(audioUrl)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
        isPlaying = true
        binding.playPauseBtn.setImageResource(R.drawable.ic_pause_btn)
    }

    private fun stopPlayback() {
        exoPlayer?.stop()
        isPlaying = false
        binding.playPauseBtn.setImageResource(R.drawable.ic_play_btn)
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
