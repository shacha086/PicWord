package com.shacha.picword

import androidx.databinding.ObservableField
import java.io.File

data class DocumentCardDataHolder(
    val fileName: ObservableField<String>,
    val file: ObservableField<File>
)