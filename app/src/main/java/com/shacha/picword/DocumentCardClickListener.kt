package com.shacha.picword

import android.content.Intent
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.shacha.picword.Util.MS_WORD
import com.shacha.picword.Util.checkFileNameInvalid
import com.shacha.picword.Util.div
import com.shacha.picword.Util.getUriForFile
import com.shacha.picword.Util.shareFile

interface DocumentCardClickListener {
    fun onOpenClicked(view: View, holder: DocumentCardDataHolder)
    fun onShareClicked(view: View, holder: DocumentCardDataHolder)
    fun onDeleteClicked(view: View, holder: DocumentCardDataHolder)
    fun onNameLongClicked(view: View, holder: DocumentCardDataHolder): Boolean
}

object DocumentCardClickListenerImpl : DocumentCardClickListener {
    override fun onOpenClicked(view: View, holder: DocumentCardDataHolder) = with(view.context) context@{
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(
                this@context.getUriForFile(getString(R.string.authorities), holder.file.get()!!),
                MS_WORD
            )
        })
    }

    override fun onShareClicked(view: View, holder: DocumentCardDataHolder) = with(view.context) {
        shareFile(getUriForFile(getString(R.string.authorities), holder.file.get()!!), "Share")
    }

    override fun onDeleteClicked(view: View, holder: DocumentCardDataHolder) = with(view.context) {
        if (holder.file.get()!!.delete()) {
            (view.parent.parent.parent as View).visibility = View.GONE
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNameLongClicked(view: View, holder: DocumentCardDataHolder) = with(view.context) {
        MaterialDialog(this)
            .title(text = "重命名")
            .input(
                allowEmpty = false,
                prefill = holder.fileName.get(),
                waitForPositiveButton = false,
                callback = ::checkFileNameInvalid
            )
            .negativeButton(text = "取消")
            .positiveButton(text = "完成") { dialog ->
                val input = dialog.getInputField().text
                val file = holder.file.get()!!
                val dest = file.parentFile!! / "$input.docx"
                if (file.renameTo(dest)) {
                    holder.fileName.set(input.toString())
                    holder.file.set(dest)
                } else {
                    Toast.makeText(this, "重命名失败", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
        true
    }
}