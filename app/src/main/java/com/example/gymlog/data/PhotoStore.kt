package com.example.gymlog.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.ExifInterface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class PhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, ROOT).apply { mkdirs() }
    private val originals = File(root, PhotoPaths.ORIGINALS).apply { mkdirs() }
    private val edited = File(root, PhotoPaths.EDITED).apply { mkdirs() }
    private val thumbs = File(appContext.cacheDir, THUMBS).apply { mkdirs() }

    fun captureFile(): File {
        val dir = File(appContext.cacheDir, ROOT).apply { mkdirs() }
        return File(dir, CAPTURE)
    }

    fun captureUri(): Uri {
        val file = captureFile().apply { parentFile?.mkdirs(); if (!exists()) createNewFile() }
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    fun importUri(uri: Uri, id: String): String? {
        val relative = PhotoPaths.original(id)
        val target = fileFor(relative) ?: return null
        val copied = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return null
            target.length() > 0L
        }.getOrDefault(false)
        if (!copied) {
            target.delete()
            return null
        }
        return relative
    }

    fun importCapture(id: String): String? {
        val capture = captureFile()
        if (!capture.isFile || capture.length() <= 0L) return null
        return importUri(Uri.fromFile(capture), id).also { capture.delete() }
    }

    fun discardCapture() {
        captureFile().delete()
    }

    fun deleteRelative(relative: String?) {
        fileFor(relative)?.delete()
    }

    fun fileFor(relative: String?): File? {
        if (!PhotoPaths.isSafe(relative)) return null
        val file = File(root, relative!!.replace('\\', '/'))
        val rootPath = root.canonicalFile
        val target = file.canonicalFile
        if (!target.path.startsWith(rootPath.path + File.separator) && target != rootPath) return null
        target.parentFile?.mkdirs()
        return target
    }

    fun decodeDisplay(relative: String?): Bitmap? {
        val file = fileFor(relative) ?: return null
        if (!file.isFile) return null
        return runCatching { decodeFile(file) }.getOrNull()
    }

    fun writeJpeg(relative: String, bitmap: Bitmap): Boolean {
        val file = fileFor(relative) ?: return false
        val temp = File(file.parentFile, file.name + ".tmp")
        val written = runCatching {
            FileOutputStream(temp).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) return false
            }
            val check = BitmapFactory.decodeFile(temp.absolutePath) ?: return false
            val ok = check.width > 0 && check.height > 0
            check.recycle()
            if (!ok) return false
            if (file.exists() && !file.delete()) return false
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
            file.isFile && file.length() > 0L
        }.getOrDefault(false)
        if (!written) temp.delete()
        return written
    }

    fun thumbnail(id: String, displayRelative: String?): Bitmap? {
        val cached = File(thumbs, "$id.jpg")
        if (cached.isFile) {
            BitmapFactory.decodeFile(cached.absolutePath)?.let { return it }
        }
        val source = decodeDisplay(displayRelative) ?: return null
        val edge = 320
        val scale = edge.toFloat() / maxOf(source.width, source.height).coerceAtLeast(1)
        val thumb = if (scale >= 1f) {
            source
        } else {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { if (it != source) source.recycle() }
        }
        runCatching {
            FileOutputStream(cached).use { thumb.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        }
        return thumb
    }

    fun deleteThumbnail(id: String) {
        File(thumbs, "$id.jpg").delete()
    }

    /** Deletes private files that no saved row references. */
    fun deleteOrphans(referenced: Set<String>) {
        listOf(originals, edited).forEach { folder ->
            folder.listFiles()?.forEach { file ->
                val relative = "${folder.name}/${file.name}"
                if (file.isFile && relative !in referenced && file.extension == "jpg" && !file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
        }
    }

    private fun decodeFile(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoImage.sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val degrees = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val turned = PhotoImage.rotate(decoded, PhotoImage.degreesFromExif(degrees))
        if (turned != decoded) decoded.recycle()
        return turned
    }

    companion object {
        const val ROOT = "photo_wall"
        private const val THUMBS = "photo_wall_thumbs"
        private const val CAPTURE = "capture.jpg"
    }
}
