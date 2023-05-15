package com.not_example.network_1ch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URL

interface MessageRepository {
    suspend fun loadImage(path: String, link: String): Bitmap?

    suspend fun loadMessages(
        startId: Long,
        limit: Int,
        reverse: Boolean
    ): List<MessageDbEntry>
}

class DefaultMessageRepository(
    private val messageDao: MessageDao,
    private val serverAPI: ServerAPI
) : MessageRepository {
    override suspend fun loadImage(path: String, link: String) =
        withContext(Dispatchers.IO) {
            var res: Bitmap? = null
            val f = File(path)
            if (f.exists()) {
                FileInputStream(f).use {
                    res = BitmapFactory.decodeStream(it)
                }
            } else {
                if (f.parentFile != null && !f.parentFile!!.exists()) {
                    f.parentFile!!.mkdirs()
                }
                f.createNewFile()
                try {
                    withContext(NonCancellable) { // to save the image if it was loaded
                        res =
                            BitmapFactory.decodeStream(URL(link).openStream())
                        res!!.compress(Bitmap.CompressFormat.PNG, 100, f.outputStream())
                    }
                } finally {
                    if (res == null || f.length() == 0L) {
                        f.delete()
                    }
                }
            }
            return@withContext res
        }

    override suspend fun loadMessages(
        startId: Long,
        limit: Int,
        reverse: Boolean
    ): List<MessageDbEntry> = withContext(Dispatchers.IO) {
        val dbRes: List<MessageDbEntry> = if (!reverse) {
            messageDao.getNext(startId, limit)
        } else {
            messageDao.getPrev(startId, limit)
        }
        val good = correct(dbRes, startId, reverse)
        if (good != 0) {
            return@withContext dbRes.take(good)
        }
        Log.i("LoadMessages", "Network query")
        val networkRes = serverAPI.getMessages(limit, startId, reverse)
        saveToDb(dbRes, networkRes, startId, reverse)
        return@withContext networkRes
    }

    private fun correct(
        data: List<MessageDbEntry>,
        startId: Long,
        reverse: Boolean = false
    ): Int {
        for (i: Int in data.indices) {
            val prev = if (i > 0) data[i - 1].id else startId
            if ((!reverse && !(((data[i].prevMki
                    ?: data[i].id) <= prev))) || (reverse && !(((data[i].nextMki
                    ?: data[i].id) >= prev)))
            ) {
                return i
            }
        }
        return data.size
    }

    private suspend fun saveToDb(
        dbRes: List<MessageDbEntry>,
        networkRes: List<MessageDbEntry>,
        startId: Long,
        reverse: Boolean
    ) {
        if (networkRes.isEmpty()) {
            return
        }
        val cachedLast = dbRes.find { it.id == networkRes.last().id } ?: networkRes.last()
        for (i: Int in networkRes.indices) {
            if (!reverse) {
                networkRes[i].prevMki = if (i > 0) networkRes[i - 1].id else startId
                networkRes[i].nextMki =
                    if (i + 1 < networkRes.size) networkRes[i + 1].id else cachedLast.nextMki
            } else {
                networkRes[i].nextMki = if (i > 0) networkRes[i - 1].id else startId
                networkRes[i].prevMki =
                    if (i + 1 < networkRes.size) networkRes[i + 1].id else cachedLast.prevMki
            }
        }
        val startMsg = messageDao.getNext(if (!reverse) startId - 1 else startId + 1, 1)
        messageDao.insertMessages(networkRes)
        if (startMsg.isNotEmpty() && startMsg.first().id == startId) {
            if (!reverse) {
                startMsg.first().nextMki = networkRes.first().id
            } else {
                startMsg.first().prevMki = networkRes.first().id
            }
            messageDao.insertMessages(startMsg)
        }
    }
}