package com.not_example.network_1ch

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.max

class MessagesModel(private val repo: MessageRepository) : ViewModel() {
    private val mMessages: MutableList<MessageUiData> = mutableListOf()
    val messages = MutableLiveData<List<MessageUiData>>(mMessages)

    var lrid: Long = Constants.FIRST_KNOWN_ID
    var cachePath: String? = null

    suspend fun loadNextMessages(): SafeResponse<Int> {
        return loadMessages(false)
    }

    suspend fun loadPrevMessages(): SafeResponse<Int> {
        return loadMessages(true)
    }

    private suspend fun loadMessages(reverse: Boolean): SafeResponse<Int> =
        withContext(Dispatchers.Default) {
            try {
                val startId = if (!reverse) {
                    if (mMessages.isNotEmpty()) mMessages.last().id else lrid
                } else {
                    if (mMessages.isNotEmpty()) mMessages.first().id else lrid
                }
                val res = repo.loadMessages(startId, Constants.MSG_LOAD_LIMIT, reverse)
                val needToAdd = if (!reverse) {
                    (mMessages.isEmpty() || mMessages.last().id < res.first().id)
                } else {
                    (mMessages.isEmpty() || res.last().id < mMessages.first().id)
                }
                if (res.isNotEmpty() && needToAdd) {
                    if (!reverse) {
                        mMessages.addAll(convertMessages(res))
                        shrinkFirst()
                    } else {
                        mMessages.addAll(0, convertMessages(res).reversed())
                        shrinkLast()
                    }
                    messages.postValue(mMessages)
                }
                return@withContext SafeResponse(res.size)
            } catch (e: HttpException) {
                return@withContext SafeResponse(
                    e,
                    Resources.getSystem().getString(
                        R.string.generic_http_error,
                        SafeResponse.localizedHttpMessage(e.code()),
                        "LoadMessages"
                    )
                )
            } catch (e: ConnectException) {
                Log.e("LoadMessages", e.message.toString())
                return@withContext SafeResponse(e)
            } catch (e: Exception) {
                Log.e("LoadMessages", e.message.toString())
                return@withContext SafeResponse(0)
            }
        }

    private suspend fun loadThumb(filename: String?, imageLink: String?): SafeResponse<Bitmap?> {
        if (filename != null && imageLink != null) {
            val path = "$cachePath/${Constants.THUMB_CACHE_DIR}/$filename"
            val link = "${Constants.THUMB_PATH}/$imageLink"
            try {
                var res: Bitmap? = null
                if (imageLink.substringAfterLast('.', "").isEmpty()) {
                    // process images without extensions (server problem)
                    val extensions = listOf("png", "JPEG", "jpg")
                    for (i: Int in extensions.indices) {
                        val ext = extensions[i]
                        try {
                            res = repo.loadImage(path, "$link.$ext")
                            break
                        } catch (e: Exception) {
                            Log.e("LoadThumb", "$filename: " + e.message.toString())
                            if (i + 1 == extensions.size) {
                                throw e
                            }
                        }
                    }

                } else {
                    res = repo.loadImage(path, link)
                }
                return SafeResponse(res)
            } catch (e: HttpException) {
                Log.e("LoadThumb", "$filename: " + e.message.toString())
                return SafeResponse(
                    e,
                    Resources.getSystem().getString(
                        R.string.generic_http_error,
                        SafeResponse.localizedHttpMessage(e.code()),
                        "LoadThumb"
                    )
                )
            } catch (e: Exception) {
                Log.e("LoadThumb", "$filename: " + e.message.toString())
                return SafeResponse(e)
            }
        }
        return SafeResponse(null)
    }

    private fun convertMessages(list: List<MessageDbEntry>): List<MessageUiData> {
        return list.map { msg ->
            MessageUiData(
                msg.id,
                msg.time,
                msg.from,
                msg.data.text?.text,
                { loadThumb(msg.data.image?.filename, msg.data.image?.link) },
                if (msg.data.image == null) null
                else Pair(
                    "$cachePath/${Constants.DETAIL_CACHE_DIR}/${msg.data.image.filename}",
                    "${Constants.DETAIL_PATH}/${msg.data.image.link}"
                )
            )
        }
    }

    private fun shrinkFirst() {
        val n = max(0, mMessages.size - Constants.BUFFER_SIZE)
        if (n > Constants.BUFFER_SIZE) {
            shrink(0, n)
        }
    }

    private fun shrinkLast() {
        val n = max(0, mMessages.size - Constants.BUFFER_SIZE)
        if (n > Constants.BUFFER_SIZE) {
            shrink(mMessages.size - n, mMessages.size)
        }
    }

    private fun shrink(l: Int, r: Int) {
        Log.i("ShrinkMessages", "shrink: ${mMessages.size}, remove ($l, $r)")
        assert(l in 0 until r && r <= mMessages.size)
        mMessages.subList(l, r).clear()
    }

    fun isEmpty(): Boolean {
        return mMessages.isEmpty()
    }

    fun checkMessagesOrder(): Long {
        var invalidId = 0L
        for (i: Int in 1 until mMessages.size) {
            if (mMessages[i].id <= mMessages[i - 1].id) {
                invalidId = mMessages[i].id
                Log.e("CheckOrder", "Invalid id: ${mMessages[i - 1].id} and ${mMessages[i].id}")
            }
            if (mMessages[i].id - mMessages[i - 1].id != 1L) {
                Log.i("CheckOrder", "Sussy Baka: ${mMessages[i - 1].id} and ${mMessages[i].id}")
            }
        }
        return invalidId
    }
}