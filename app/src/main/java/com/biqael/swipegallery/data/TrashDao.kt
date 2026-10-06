package com.biqael.swipegallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash_item ORDER BY dateAdded DESC")
    suspend fun getAll(): List<TrashItem>

    @Query("SELECT * FROM trash_item WHERE id = :id")
    suspend fun getById(id: Long): TrashItem?

    @Query("SELECT * FROM trash_item WHERE dateAdded < :threshold")
    suspend fun getOlderThan(threshold: Long): List<TrashItem>

    @Query("SELECT SUM(sizeBytes) FROM trash_item")
    suspend fun getTotalSize(): Long?

    @Query("SELECT COUNT(*) FROM trash_item")
    suspend fun getCount(): Int

    @Insert
    suspend fun insert(item: TrashItem): Long

    @Query("DELETE FROM trash_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trash_item")
    suspend fun deleteAll()
}
