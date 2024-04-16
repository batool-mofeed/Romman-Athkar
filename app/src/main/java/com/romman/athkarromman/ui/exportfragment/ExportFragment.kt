package com.romman.athkarromman.ui.exportfragment

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
                "-c:v", "mpeg4",
                "-c:a", "aac",
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
        if (audioUrl == null) {
            showProgressDialog()
        }

        val wrappedText = getWrappedText(text)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val resources = requireContext().resources
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_export_img)
                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                val canvas = Canvas(mutableBitmap)
//                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//                paint.color = Color.BLACK
//                paint.textSize = 33F

                val bounds = Rect()
//                paint.getTextBounds(wrappedText.joinToString("\n"), 0, text.length, bounds)

                // Calculate x and y coordinates to center the text
                val x = (canvas.width - bounds.width()) / 50
                val y = (canvas.height + bounds.height()) / 2.5

                val mTextLayout = StaticLayout(
                    wrappedText.joinToString ("\n"),
                    TextPaint().apply { textSize = 50f },
                    canvas.width,
                    Layout.Alignment.ALIGN_CENTER,
                    1.0f,
                    0.0f,
                    false
                )
                canvas.save()
                canvas.translate(x.toFloat() ,y.toFloat())
                mTextLayout.draw(canvas)
                canvas.restore()

                val imageFile = saveBitmapAsImage(mutableBitmap)

                launch(Dispatchers.Main) {
                    if (audioUrl == null) {
                        downloadFile(imageFile, "image/jpeg")
                    } else {
                        mergeAudioWithVideo(audioUrl, imageFile)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    hideProgressDialog()
                    // Handle failure
                }
            }
        }
    }

    private fun getWrappedText(text: String): MutableList<String> {
        val maxWordsPerLine = 8
        val lines = mutableListOf<String>()
        var currentLine = ""
        text.split(" ").forEachIndexed { index, word ->
            if (index % maxWordsPerLine == 0 && index > 0) {
                lines.add(currentLine.trim()) // Trim to remove leading/trailing whitespace
                currentLine = word
            } else {
                currentLine += if (currentLine.isEmpty()) word else " $word"
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.trim())
        }
        return lines
    }

    private fun saveBitmapAsImage(bitmap: Bitmap): File {
        val imageFileName = "${System.currentTimeMillis()}_text_added.jpg"
        val imagesDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        val imageFile = File(imagesDir, imageFileName)
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        return imageFile
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
                            hideProgressDialog()
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