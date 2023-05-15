package com.not_example.network_1ch

import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.File
import java.io.InputStream

class MessageService : Service() {
    private val binder = LocalBinder()
    private val mScope = CoroutineScope(Dispatchers.IO + Job())
    private var mClient: MessageServiceClient? = null

    interface MessageServiceClient {
        fun processSafeResponse(res: SafeResponse<Unit>)
    }

    override fun onDestroy() {
        super.onDestroy()
        mScope.cancel()
        mClient = null
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): MessageService {
            return this@MessageService
        }

        fun setClient(client: MessageServiceClient?) {
            mClient = client
        }
    }

    fun sendMessage(msg: String?) {
        mScope.launch {
            try {
                val reqMsg = RequestBody.create(
                    MediaType.parse("application/json; charset=UTF-8"),
                    "{\"from\":\"${Constants.USERNAME}\",\"data\":{\"Text\":{\"text\":\"$msg\"}}}"
                )
                (application as MessagesApplication).serverAPI.sendMessage(reqMsg).use {
                    Log.i("SendTextMessage", it.string())
                }
            } catch (e: HttpException) {
                Log.e("SendTextMessage", e.message.toString())
                mClient?.processSafeResponse(
                    SafeResponse(
                        e, Resources.getSystem().getString(
                            R.string.generic_http_error,
                            SafeResponse.localizedHttpMessage(e.code()),
                            "SendTextMessage"
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e("SendTextMessage", e.message.toString())
            }
        }
    }

    fun sendImage(source: InputStream?) {
        mScope.launch {
            try {
                val f = File(kotlin.io.path.createTempFile("img", ".png").toString())
                try {
                    BitmapFactory.decodeStream(source)
                        .compress(Bitmap.CompressFormat.PNG, 100, f.outputStream())
                    val reqFile = RequestBody.create(MediaType.parse("multipart/form-data"), f)
                    val body: MultipartBody.Part =
                        MultipartBody.Part.createFormData("upload", f.name, reqFile)
                    val reqMsg = RequestBody.create(
                        MediaType.parse("application/json; charset=UTF-8"),
                        "{\"from\":\"${Constants.USERNAME}\"}"
                    )
                    (application as MessagesApplication).serverAPI.sendImage(reqMsg, body).use {
                        Log.i("SendImage", it.string())
                    }
                } finally {
                    f.delete()
                }
            } catch (e: HttpException) {
                Log.e("SendImage", e.message.toString())
                mClient?.processSafeResponse(
                    SafeResponse(
                        e, Resources.getSystem().getString(
                            R.string.generic_http_error,
                            SafeResponse.localizedHttpMessage(e.code()),
                            "SendImage"
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e("SendImage", e.message.toString())
            }
        }
    }
}