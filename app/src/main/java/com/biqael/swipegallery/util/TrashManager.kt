package com.biqael.swipegallery.util

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.biqael.swipegallery.data.AppDatabase
import com.biqael.swipegallery.data.TrashItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class TrashManager(private val context: Context) {

    private val dao = AppDatabase.get(context).trashDao()

    private val trashDir: File
        get() = File(context.filesDir, "trash").apply { if (!exists()) mkdirs() }

    suspend fun copyToTrash(photo: PhotoInfo, bucketName: String): TrashItem? =
        withContext(Dispatchers.IO) {
            try {
                val ext = extFromMime(photo.mimeType)
                val fileName = "trash_${UUID.randomUUID()}.$ext"
                val file = File(trashDir, fileName)

                context.contentResolver.openInputStream(photo.uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null

                val item = TrashItem(
                    originalUri = photo.uri.toString(),
                    originalBucketId = photo.bucketId,
                    originalBucketName = bucketName,
                    trashFileName = fileName,
                    mimeType = photo.mimeType,
                    sizeBytes = file.length(),
                    dateAdded = System.currentTimeMillis()
                )
                val id = dao.insert(item)
                item.copy(id = id)
            } catch (e: Exception) {
                null
            }
        }

    suspend fun deletePermanently(item: TrashItem) = withContext(Dispatchers.IO) {
        File(trashDir, item.trashFileName).delete()
        dao.deleteById(item.id)
    }

    suspend fun rollback(item: TrashItem) = withContext(Dispatchers.IO) {
        File(trashDir, item.trashFileName).delete()
        dao.deleteById(item.id)
    }

    suspend fun deleteAllTrash() = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { it.delete() }
        dao.deleteAll()
    }

    suspend fun restore(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(trashDir, item.trashFileName)
            if (!file.exists()) {
                dao.deleteById(item.id)
                return@withContext false
            }

            val ext = extFromMime(item.mimeType)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "restored_${System.currentTimeMillis()}.$ext")
                put(MediaStore.Images.Media.MIME_TYPE, item.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SwipeGallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return@withContext false

            context.contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)

            file.delete()
            dao.deleteById(item.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAll(): List<TrashItem> = dao.getAll()
    suspend fun getCount(): Int = dao.getCount()
    suspend fun getTotalSize(): Long = dao.getTotalSize() ?: 0L

    suspend fun autoClean(maxAgeMs: Long, maxBytes: Long): Int = withContext(Dispatchers.IO) {
        var cleaned = 0
        val threshold = System.currentTimeMillis() - maxAgeMs
        dao.getOlderThan(threshold).forEach { item ->
            File(trashDir, item.trashFileName).delete()
            dao.deleteById(item.id)
            cleaned++
        }

        var total = dao.getTotalSize() ?: 0L
        if (total > maxBytes) {
            val all = dao.getAll().sortedBy { it.dateAdded }
            for (item in all) {
                if (total <= maxBytes) break
                File(trashDir, item.trashFileName).delete()
                dao.deleteById(item.id)
                total -= item.sizeBytes
                cleaned++
            }
        }
        cleaned
    }

    private fun extFromMime(mime: String): String = when (mime.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/bmp" -> "bmp"
        "image/heic", "image/heif" -> "heic"
        else -> "jpg"
    }
}
