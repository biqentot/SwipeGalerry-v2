package com.biqael.swipegallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_item")
data class TrashItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalUri: String,
    val originalBucketId: String,
    val originalBucketName: String,
    val trashFileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAdded: Long
)
