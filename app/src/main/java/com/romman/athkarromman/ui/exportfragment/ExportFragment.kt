package com.romman.athkarromman.ui.exportfragment

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.romman.athkarromman.R
import com.romman.athkarromman.databinding.FragmentExportBinding
import com.romman.athkarromman.ui.exportsheet.ExportSheet
import com.romman.athkarromman.utils.PermissionsHelper
import com.romman.athkarromman.utils.buildProgressDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class ExportFragment : Fragment() {

    private var viewmodel: ExportViewModel? = null
    lateinit var binding: FragmentExportBinding

    private var progressDialog: Dialog? = null

    private val args by navArgs<ExportFragmentArgs>()

    private val permissionHelper by lazy {
        PermissionsHelper(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val exportViewModel by viewModels<ExportViewModel>()
        viewmodel = exportViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_export, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClicks()
        binding.athkarTxt.text = args.text
    }

    private fun initClicks() {
        with(binding) {
            exportBtn.setOnClickListener {
                ExportSheet.newInstance({
                    permissionHelper.checkWriteStoragePermission({
                        addTextToImageAndDownload(args.text)
                    })
                }, {
                    addTextToImageAndDownload(args.text, args.link)
                }).show(parentFragmentManager, null)
            }
            backBtn.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun mergeAudioWithVideo(
        audioUrl: String,
        imgFile: File
    ) {
        showProgressDialog()

        GlobalScope.launch(Dispatchers.IO) {
            val mergedVideoFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val imagesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                File(imagesDir, "${System.currentTimeMillis()}_merged.mp4")
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, "${System.currentTimeMillis()}_merged.mp4")
            }

            val command = arrayOf(
                "-loop", "1",
                "-i", imgFile.absolutePath,
                "-i", audioUrl,
                "-c:v", "libx264",
                "-c:a", "copy",
                "-strict", "normal",
                "-shortest",
                "-preset", "superfast",
                "-movflags", "faststart",
                "-map",
                "0:v:0",
                "-map",
                "1:a:0",
                mergedVideoFile.absolutePath
            )

            val rc = FFmpeg.execute(command)

            if (rc == RETURN_CODE_SUCCESS) {
                Timber.e("Audio merged with video successfully.")
                launch(Dispatchers.Main) {
                    hideProgressDialog()
                    downloadFile(mergedVideoFile, "video/mp4")
                }
            } else {
                Timber.e("Failed to merge audio with video. RC=$rc")
                launch(Dispatchers.Main) {
                    hideProgressDialog()
                    // Handle failure
                }
            }
        }
    }


    private fun addTextToImageAndDownload(text: String, audioUrl: String? = null) {
        showProgressDialog()

        GlobalScope.launch(Dispatchers.IO) {
            val finalImageFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val imagesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                File(imagesDir, "${System.currentTimeMillis()}_text_added.jpg")
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, "${System.currentTimeMillis()}_text_added.jpg")
            }

            val saveDeferred = async { saveImage() }
            saveDeferred.await() // Wait for saveImage() to complete


            /// to not make all text in one line
            val maxWordsPerLine = 9
            val lines = mutableListOf<String>()
            var currentLine = ""
            text.split(" ").forEachIndexed { index, word ->
                if (index % maxWordsPerLine == 0 && index > 0) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    currentLine += if (currentLine.isEmpty()) word else " $word"
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            val wrappedText = lines.joinToString("\n")
            //end of lines

            val command = arrayOf(
                "-i",
                "${requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/bg_athkar.png",
                "-vf",
                "drawtext=text_shaping=1:text='$wrappedText':x=100:y=200:fontsize=24:fontcolor=black:fontfile=/system/fonts/DroidSans-Bold.ttf",
                finalImageFile.absolutePath
            )


            val rc = FFmpeg.execute(command)

            if (rc == RETURN_CODE_SUCCESS) {
                println("Command execution completed successfully.")
                launch(Dispatchers.Main) {
                    hideProgressDialog()
                    downloadFile(finalImageFile, "image/jpeg")
                }
            } else {
                println("Command execution failed with rc=$rc")
                launch(Dispatchers.Main) {
                    hideProgressDialog()
                    // Handle failure
                }
            }

            if (audioUrl != null) {
                mergeAudioWithVideo(audioUrl, finalImageFile)
            }
        }
    }

    private fun saveImage() {
        val bm = BitmapFactory.decodeResource(
            resources,
            R.drawable.bg_export_img
        )
        val imagesDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        }
        val file = File(imagesDir, "bg_athkar.png")
        if (!file.exists()) {
            try {
                val outStream = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: Exception) {
                println("rrrrrrrrrrrrrrr ${e.message}")
                e.printStackTrace()
            }
        }
    }


    private suspend fun downloadFile(file: File, mimeType: String) {
        withContext(Dispatchers.IO) {
            val inputStream = FileInputStream(file)
            val resolver = requireContext().contentResolver

            // Set up values for inserting into MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            // Insert file into MediaStore
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { outputStream ->
                resolver.openOutputStream(outputStream)?.use { output ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    try {
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                        // Display Toast message on the main thread
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Downloaded successfully to gallery",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        // Handle exception
                    } finally {
                        inputStream.close()
                        output.close()
                    }
                }
            }
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


}