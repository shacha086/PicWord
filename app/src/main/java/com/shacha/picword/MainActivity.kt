package com.shacha.picword

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.isPhotoPickerAvailable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.getCheckBoxPrompt
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.isItemChecked
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.luck.picture.lib.config.MediaType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.luck.picture.lib.model.PictureSelector
import com.shacha.picword.Units.Companion.cm
import com.shacha.picword.Units.Companion.div
import com.shacha.picword.Units.Companion.minus
import com.shacha.picword.Units.Companion.px
import com.shacha.picword.Util.DEFAULT_PATTERN
import com.shacha.picword.Util.applyMatrix
import com.shacha.picword.Util.checkFileNameInvalid
import com.shacha.picword.Util.div
import com.shacha.picword.Util.formatTime
import com.shacha.picword.Util.get
import com.shacha.picword.Util.getFragment
import com.shacha.picword.Util.getParcelableArrayListExtraSafe
import com.shacha.picword.Util.getParcelableExtraSafe
import com.shacha.picword.Util.getUriForFile
import com.shacha.picword.Util.shareFile
import com.shacha.picword.Util.toByteArrayOutputStream
import com.shacha.picword.ZipScope.Companion.ZipScope
import com.shacha.picword.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.poifs.filesystem.FileMagic
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.util.zip.ZipInputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.topAppBar)
        theme.applyStyle(
            rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference,
            true
        )
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topAppBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        when (intent?.action) {
            Intent.ACTION_SEND_MULTIPLE -> {
                if (intent!!.type?.startsWith("image/") != true) {
                    return
                }
                intent.getParcelableArrayListExtraSafe<Uri>(Intent.EXTRA_STREAM)?.let {
                    val files = it.asSequence().map { uri ->
                        contentResolver.openInputStream(uri) ?: throw NullPointerException()
                    }
                    filesToDocx(files)
                }
            }

            Intent.ACTION_SEND -> {
                if (intent!!.type?.equals("application/vnd.android.package-archive") != true) {
                    return
                }
                intent.getParcelableExtraSafe<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    contentResolver.openInputStream(uri).use { input ->
                        ZipInputStream(input).use { zip ->
                            ZipScope(zip) {
                                val files = mutableListOf<InputStream>()
                                forEach {
                                    val stream = ByteArrayOutputStream()
                                    zip.copyTo(stream)
                                    files.add(ByteArrayInputStream(stream.toByteArray()))
                                }
                                if (files.any()) {
                                    filesToDocx(files.asSequence())
                                }
                            }
                        }
                    }
                }

            }

            else -> {
                return
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onFabClick(view: View) {
        pickPhotoCompat()
    }

    private fun pickPhotoCompat() {
        if (isPhotoPickerAvailable(this)) {
            // use photo picker
            pickImage()
        } else {
            PictureSelector.create(this)
                .openGallery(MediaType.IMAGE)
                .setImageEngine(GlideEngine)
                .forResult(object : OnResultCallbackListener {
                    override fun onResult(result: List<LocalMedia>) {
                        val files = result.asSequence().map {
                            contentResolver.openInputStream(it.path!!.toUri())
                                ?: throw NullPointerException()
                        }
                        filesToDocx(files)
                    }

                    override fun onCancel() {
                    }
                })
        }
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { result ->
            if (result.isNotEmpty()) {
                val files = result.asSequence().map {
                    contentResolver.openInputStream(it) ?: throw NullPointerException()
                }
                filesToDocx(files)
            }
        }

    private fun pickImage() =
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.set_filename -> {
                MaterialDialog(this)
                    .title(text = "设置默认文件名")
                    .input(
                        allowEmpty = true,
                        prefill = getSharedPreferences(
                            "settings",
                            MODE_PRIVATE
                        ).get("pattern", DEFAULT_PATTERN),
                        waitForPositiveButton = false,
                        callback = ::checkFileNameInvalid
                    )
                    .negativeButton(text = "取消")
                    .positiveButton(text = "完成") {
                        val input = it.getInputField().text
                        getSharedPreferences("settings", MODE_PRIVATE)
                            .edit(commit = true) {
                                if (input.isEmpty()) {
                                    remove("pattern")
                                } else {
                                    putString("pattern", input.toString())
                                }
                            }
                    }
                    .show()
                true
            }

            R.id.about -> {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://github.com/shacha086/PicWord/")
                })
                true
            }

            else -> {
                false
            }
        }
    }

    private fun onFileNameEntered(files: Sequence<InputStream>, dialog: MaterialDialog) {
        lifecycleScope.launch(Dispatchers.IO) {
            var fileName = dialog.getInputField().text.toString()
            val needRotate = !dialog.isItemChecked(0)
            val exists = (filesDir / "$fileName.docx").exists()
            val overwrite = if (exists) {
                withContext(Dispatchers.Main) {
                    suspendCoroutine {
                        MaterialDialog(this@MainActivity).show {
                            message(text = "文件已存在。要怎么做？")
                            positiveButton(text = "覆盖") { _ ->
                                it.resume(true)
                            }
                            negativeButton(text = "重命名") { _ ->
                                it.resume(false)
                            }
                        }
                    }
                }
            } else false
            if (exists && !overwrite) {
                while ((filesDir / "$fileName.docx").exists()) {
                    val result = "\\(\\d+(\\))\$".toRegex().find(fileName)
                    if (result != null) {
                        val number = result.value.run { substring(1, length - 1) }
                        fileName = fileName.substring(0, fileName.length - number.length - 2) + "(${number.toUInt() + 1u})"
                    } else {
                        fileName += " (1)"
                    }
                }
            }
            val outputFile = filesDir / "$fileName.docx"
            val rotatedFiles = if (needRotate) {
                getRotatedFiles(dialog, files)
            } else {
                files.asFlow()
            }
            writeDocx(rotatedFiles, outputFile)

            val fileURI = getUriForFile(
                getString(R.string.authorities),
                outputFile
            )
            withContext(Dispatchers.Main) {
                if (!overwrite) {
                    supportFragmentManager.getFragment(FirstFragment::class.java)?.addDocumentCard(layoutInflater, outputFile, 0)
                }
                shareFile(fileURI, "Share")
            }
        }
    }

    private fun getRotatedFiles(
        dialog: MaterialDialog,
        files: Sequence<InputStream>
    ): Flow<InputStream> {
        val rotateType = if (dialog.isItemChecked(1)) {
            RotateType.ROTATE_TO_LANDSCAPE
        } else {
            RotateType.ROTATE_TO_PORTRAIT
        }
        var rememberDirection: RotateDirection? = null
        return files.asFlow().map { stream ->
            (if (stream.markSupported()) stream else ByteArrayInputStream(stream.use { it.readBytes() })).let { cacheStream ->
                val img = BitmapFactory.decodeStream(cacheStream)
                cacheStream.reset()
                val width = img.width
                val height = img.height
                if (width == height) {
                    return@map cacheStream
                }

                if (width > height && rotateType == RotateType.ROTATE_TO_LANDSCAPE) {
                    return@map cacheStream
                }

                if (width < height && rotateType == RotateType.ROTATE_TO_PORTRAIT) {
                    return@map cacheStream
                }

                val direction =
                    rememberDirection ?: try {
                        askRotation(
                            img,
                            onSelected = { dialog, direction ->
                                val isRemember = dialog.getCheckBoxPrompt().isChecked
                                if (isRemember) {
                                    rememberDirection = direction
                                }
                                resume(direction)
                            }
                        )
                    } catch (e: CancellationException) {
                        throw e
                    }

                if (direction == RotateDirection.IGNORE) {
                    return@map cacheStream
                }

                cacheStream.close()
                val matrix = when (direction) {
                    RotateDirection.CLOCKWISE -> {
                        Matrix().apply { postRotate(90f) }
                    }

                    RotateDirection.COUNTERCLOCKWISE -> {
                        Matrix().apply { postRotate(-90f) }
                    }

                    else -> throw IllegalStateException()
                }
                val byteArray =
                    img.applyMatrix(matrix, filter = true)
                        .toByteArrayOutputStream(Bitmap.CompressFormat.JPEG)
                        .toByteArray()
                val result = ByteArrayInputStream(byteArray)
                withContext(Dispatchers.IO) {
                    result
                }

            }

        }
    }

    @Suppress("DEPRECATION")
    private suspend fun askRotation(
        img: Bitmap,
        onSelected: (Continuation<RotateDirection>.(MaterialDialog, RotateDirection) -> Unit)
    ) = coroutineScope {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine {
                MaterialDialog(this@MainActivity)
                    .title(text = "旋转")
                    .customView(
                        view = ImageView(this@MainActivity).apply { setImageBitmap(img) },
                        scrollable = false
                    )
                    .positiveButton(text = RotateDirection.COUNTERCLOCKWISE.description) { dialog ->
                        onSelected(
                            it,
                            dialog,
                            RotateDirection.COUNTERCLOCKWISE
                        )
                    }
                    .negativeButton(text = RotateDirection.CLOCKWISE.description) { dialog ->
                        onSelected(
                            it,
                            dialog,
                            RotateDirection.CLOCKWISE
                        )
                    }
                    .neutralButton(text = RotateDirection.IGNORE.description) { dialog ->
                        onSelected(
                            it,
                            dialog,
                            RotateDirection.IGNORE
                        )
                    }
                    .checkBoxPrompt(text = "后续都应用此旋转") {}
                    .onCancel { _ ->
                        it.resumeWithException(CancellationException())
                    }
                    .show()
            }
        }
    }


    @SuppressLint("RestrictedApi")
    fun filesToDocx(files: Sequence<InputStream>) {
        val pattern =
            getSharedPreferences("settings", MODE_PRIVATE).get(
                "pattern",
                DEFAULT_PATTERN
            )
        val fileName = LocalDateTime.now().formatTime(pattern)
        MaterialDialog(this)
            .title(text = "请输入文件名")
            .input(
                allowEmpty = false,
                prefill = fileName,
                waitForPositiveButton = false,
                callback = ::checkFileNameInvalid
            )
            .negativeButton(text = "取消")
            .positiveButton(text = "完成") { dialog ->
                onFileNameEntered(files, dialog)
            }
            .listItemsSingleChoice(
                R.array.rotate_options,
                initialSelection = 0,
                waitForPositiveButton = false
            )
            { dialog, index, _ ->
                val message = when (index) {
                    0 -> "保持图片方向"
                    1 -> "当图片为竖向时，将询问您如何旋转"
                    2 -> "当图片为横向时，将询问您如何旋转"
                    else -> return@listItemsSingleChoice
                }
                dialog.view.contentLayout.setMessage(
                    dialog = dialog,
                    res = null,
                    text = message,
                    typeface = dialog.bodyFont,
                    applySettings = null
                )
            }
            .message(text = "保持图片方向")
            .show()
    }
}

suspend fun writeDocx(files: Flow<InputStream>, file: File) = withContext(Dispatchers.IO) {
    val outputStream = file.outputStream()
    val doc = XWPFDocument()
    val paragraph = doc.createParagraph()
    files.catch {
        outputStream.close()
        file.delete()
        throw it
    }.flowOn(Dispatchers.IO).collect { input ->
        (if (input.markSupported()) input else ByteArrayInputStream(input.use { it.readBytes() })).use { cacheStream ->
            val run = paragraph.createRun()
            val fileMagic = FileMagic.valueOf(cacheStream)
            cacheStream.reset()
            val img = BitmapFactory.decodeStream(cacheStream)
            cacheStream.reset()
            val width = img.width
            val height = img.height
            val pageWidth = (21.cm - (3.18 * 2).cm).toMillimeter()
            val pageHeight = (29.7.cm - (2.54 * 2).cm).toMillimeter()
            val imageWidth = width.px.toMillimeter()
            val imageHeight = height.px.toMillimeter()
            val scaling =
                min((pageWidth / imageWidth).toDouble(), (pageHeight / imageHeight).toDouble())
            val opWidth = (width * scaling).px.toEMU().toDouble()
            val opHeight = (height * scaling).px.toEMU().toDouble()
            println("($opWidth, $opHeight)")
            run.addPicture(
                cacheStream,
                PictureType.valueOf(fileMagic),
                "photo",
                opWidth.roundToInt(),
                opHeight.roundToInt()
            )
        }
    }
    doc.properties.coreProperties.apply {
        creator = ""
    }
    doc.properties.extendedProperties.application = "PicWord"
    doc.write(outputStream)
    outputStream.close()
}
