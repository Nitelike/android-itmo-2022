package com.not_example.network_1ch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE id > :id LIMIT :cnt")
    suspend fun getNext(id: Long, cnt: Int): List<MessageDbEntry>

    @Query("SELECT * FROM messages WHERE id < :id ORDER BY id DESC LIMIT :cnt")
    suspend fun getPrev(id: Long, cnt: Int): List<MessageDbEntry>

    @Query("SELECT COUNT (*) FROM messages")
    suspend fun cnt(): Int

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(list: List<MessageDbEntry>): LongArray
}