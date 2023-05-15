package com.not_example.network_1ch

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ImageDetailModel(private val repo: MessageRepository) : ViewModel() {
    val image: ObservableField<Bitmap> = ObservableField<Bitmap>()
    var imageLink: String? = null

    suspend fun setImage(path: String, link: String): SafeResponse<Unit> =
        withContext(Dispatchers.Default) {
            try {
                image.set(repo.loadImage(path, link))
                return@withContext SafeResponse(Unit)
            } catch (e: HttpException) {
                Log.e("DetailImage", e.message.toString())
                return@withContext SafeResponse(
                    e, Resources.getSystem().getString(
                        R.string.generic_http_error,
                        SafeResponse.localizedHttpMessage(e.code()),
                        "DetailImage"
                    )
                )
            } catch (e: Exception) {
                Log.e("DetailImage", e.message.toString())
                return@withContext SafeResponse(e)
            }
        }

    fun setImage(bitmap: Bitmap) {
        image.set(bitmap)
    }
}