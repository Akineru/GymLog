package com.example.gymlog.data

import android.content.Context
import java.io.File

open class ProfileAvatarStore(
    context: Context,
) {
    private val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    open fun file(name: String?): File? {
        if (name.isNullOrBlank() || name != FILE_NAME) return null
        val file = File(directory, name)
        return file.takeIf { it.isFile }
    }

    open fun readBytes(): ByteArray? {
        val file = file(FILE_NAME) ?: return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    open fun write(bytes: ByteArray): String {
        directory.mkdirs()
        File(directory, FILE_NAME).writeBytes(bytes)
        return FILE_NAME
    }

    open fun delete() {
        File(directory, FILE_NAME).delete()
    }

    companion object {
        const val DIRECTORY = "profile"
        const val FILE_NAME = "avatar.jpg"
    }
}
