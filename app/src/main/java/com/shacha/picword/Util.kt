package com.shacha.picword

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.luck.picture.lib.utils.FileUtils
import rikka.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Suppress("MemberVisibilityCanBePrivate", "unused")
object Util {
    const val MS_WORD = "application/msword"
    const val DEFAULT_PATTERN = "yyyy年MM月dd日HH时mm分"
    private val invalidChars = charArrayOf(
        '/',
        '?',
        '*',
        ':',
        '"',
        '\'',
        '<',
        '>',
        '`'
    )

    @Suppress("NOTHING_TO_INLINE")
    inline fun SharedPreferences.get(name: String, default: String) = getString(name, default)!!
    private val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA)
    fun LocalDateTime.formatTime(patternString: String) =
        try {
            format(DateTimeFormatter.ofPattern(patternString))!!
        } catch (e: IllegalArgumentException) {
            patternString
        }

    operator fun File.div(string: String) = File(this, string)

    inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraSafe(name: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(name)
        }

    inline fun <reified T : Parcelable> Intent.getParcelableExtraSafe(name: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(name)
        }

    fun Context.getUriForFile(authority: String, file: File): Uri =
        FileProvider.getUriForFile(this, authority, file)

    @JvmStatic
    fun formatTime(timestamp: Long): String = formatter.format(timestamp)

    fun Context.shareFile(fileURI: Uri, title: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileURI)
            type = MS_WORD
        }

        startActivity(Intent.createChooser(intent, title))
    }

    @JvmStatic
    fun formatAccurateUnitFileSize(byteSize: Long) = FileUtils.formatAccurateUnitFileSize(byteSize)

    fun checkFileNameInvalid(materialDialog: MaterialDialog, charSequence: CharSequence) {
        val inputField = materialDialog.getInputField()
        val invalidChar: Char? = invalidChars.firstOrNull { it in charSequence }

        inputField.error =
            if (invalidChar != null) "不能包含特殊字符${invalidChar}" else null
        materialDialog.setActionButtonEnabled(
            WhichButton.POSITIVE,
            invalidChar == null
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun <F : Fragment> FragmentManager.getFragment(fragmentClass: Class<F>): F? {
        val navHostFragment = this.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment

        navHostFragment.childFragmentManager.fragments.forEach {
            if (fragmentClass.isAssignableFrom(it.javaClass)) {
                return it as F
            }
        }

        return null
    }

    fun Bitmap.applyMatrix(matrix: Matrix, filter: Boolean = false) =
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, filter)

    fun Bitmap.writeToOutputStream(
        stream: OutputStream,
        format: CompressFormat = CompressFormat.PNG,
        quality: Int = 100
    ) =
        compress(format, quality, stream)

    fun Bitmap.toByteArrayOutputStream(
        format: CompressFormat = CompressFormat.PNG,
        quality: Int = 100
    ) = ByteArrayOutputStream().also { writeToOutputStream(it, format, quality) }
}