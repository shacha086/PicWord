package com.shacha.picword

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipScope private constructor(private val zipInputStream: ZipInputStream) {
    companion object {
        @Suppress("FunctionName")
        fun ZipScope(zipInputStream: ZipInputStream, block: ZipScope.() -> Unit) {
            block(ZipScope(zipInputStream))
        }
    }

    private val entries = object : Iterator<ZipEntry> {
        private var nextEntry: ZipEntry? = null

        override fun hasNext(): Boolean {
            nextEntry = zipInputStream.nextEntry
            return nextEntry != null
        }

        override fun next(): ZipEntry {
            return nextEntry ?: throw NoSuchElementException()
        }
    }

    fun forEach(operation: (ZipEntry) -> Unit) {
        for (element in this.entries) {
            operation(element)
            zipInputStream.closeEntry()
        }
    }
}

