package com.not_example.network_1ch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class Utilities(private val mDao: MessageDao) {

    suspend fun dirSize(path: String): Pair<Int, Long> = withContext(Dispatchers.Default) {
        val dir = File(path)
        var cnt = 0
        var size = 0L
        if (dir.exists()) {
            dir.walk().forEach {
                size += it.length()
                cnt++
            }
        }
        size /= 1024 * 1024
        return@withContext Pair(cnt, size)
    }

    suspend fun clearDir(path: String) = withContext(Dispatchers.Default) {
        val dir = File(path)
        if (dir.exists()) {
            dir.walk().forEach {
                it.delete()
            }
        }
    }

    private fun msgSize(msg: MessageUiData): Int {
        var res = 0
        res += Long.SIZE_BYTES // id
        res += Long.SIZE_BYTES // time
        res += msg.from.toByteArray().size // from
        res += msg.text?.toByteArray()?.size ?: 0 // text
        res += msg.detail?.first?.toByteArray()?.size ?: 0 // detail path
        res += msg.detail?.second?.toByteArray()?.size ?: 0 // detail link
        res += msg.image.get()?.byteCount ?: 0 // image
        return res
    }

    suspend fun messagesSize(list: List<MessageUiData>): Long = withContext(Dispatchers.Default) {
        var res = 0L
        for (msg in list) {
            res += msgSize(msg)
        }
        return@withContext res
    }

    suspend fun countDbEntries(): Int {
        return mDao.cnt()
    }

    suspend fun clearDb() {
        mDao.deleteAll()
    }
}