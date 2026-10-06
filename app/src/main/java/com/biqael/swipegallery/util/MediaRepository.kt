package com.biqael.swipegallery.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FolderInfo(
    val bucketId: String,
    val name: String,
    val count: Int,
    val volumeName: String
)

data class PhotoInfo(
    val id: Long,
    val uri: Uri,
    val bucketId: String,
    val volumeName: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val mimeType: String
)

class MediaRepository(private val context: Context) {

    private val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    suspend fun listVolumes(): List<String> = withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media.VOLUME_NAME),
            null, null, null
        )
        cursor?.use { c ->
            val idx = c.getColumnIndex(MediaStore.Images.Media.VOLUME_NAME)
            while (c.moveToNext()) {
                if (idx >= 0) {
                    val v = c.getString(idx) ?: continue
                    if (!result.contains(v)) result.add(v)
                }
            }
        }
        result
    }

    suspend fun listFolders(volumeFilter: String?): List<FolderInfo> = withContext(Dispatchers.IO) {
    val projection = arrayOf(
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.VOLUME_NAME
    )
    val selection = if (volumeFilter != null)
        "${MediaStore.Images.Media.VOLUME_NAME} = ?" else null
    val args = if (volumeFilter != null) arrayOf(volumeFilter) else null

    val map = LinkedHashMap<String, FolderInfo>()
    val cursor = context.contentResolver.query(collection, projection, selection, args, null)
    cursor?.use { c ->
        val idIdx = c.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val nameIdx = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val volIdx = c.getColumnIndex(MediaStore.Images.Media.VOLUME_NAME)
        while (c.moveToNext()) {
            if (idIdx < 0) continue
            val bucketId = c.getString(idIdx) ?: continue
            val name = if (nameIdx >= 0) c.getString(nameIdx) ?: bucketId else bucketId
            val vol = if (volIdx >= 0) c.getString(volIdx) ?: "external_primary" else "external_primary"
            val existing = map[bucketId]
            if (existing == null) {
                map[bucketId] = FolderInfo(bucketId, name, 1, vol)
            } else {
                map[bucketId] = existing.copy(count = existing.count + 1)
            }
        }
    }
    map.values.sortedBy { it.name.lowercase() }
}
    suspend fun listPhotos(bucketId: String?): List<PhotoInfo> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.VOLUME_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE
        )
        val selection = if (bucketId != null)
            "${MediaStore.Images.Media.BUCKET_ID} = ?" else null
        val args = if (bucketId != null) arrayOf(bucketId) else null
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val result = mutableListOf<PhotoInfo>()
        val cursor = context.contentResolver.query(collection, projection, selection, args, sort)
        cursor?.use { c ->
            val idIdx = c.getColumnIndex(MediaStore.Images.Media._ID)
            val bIdx = c.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val vIdx = c.getColumnIndex(MediaStore.Images.Media.VOLUME_NAME)
            val sIdx = c.getColumnIndex(MediaStore.Images.Media.SIZE)
            val dIdx = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val mIdx = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val uri = ContentUris.withAppendedId(collection, id)
                result.add(
                    PhotoInfo(
                        id = id,
                        uri = uri,
                        bucketId = if (bIdx >= 0) c.getString(bIdx) ?: "" else "",
                        volumeName = if (vIdx >= 0) c.getString(vIdx) ?: "" else "",
                        sizeBytes = if (sIdx >= 0) c.getLong(sIdx) else 0L,
                        dateAdded = if (dIdx >= 0) c.getLong(dIdx) * 1000L else 0L,
                        mimeType = if (mIdx >= 0) c.getString(mIdx) ?: "image/jpeg" else "image/jpeg"
                    )
                )
            }
        }
        result
    }
}
