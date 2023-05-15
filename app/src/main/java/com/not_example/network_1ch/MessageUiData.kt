package com.not_example.network_1ch

import android.graphics.Bitmap
import androidx.databinding.ObservableField

data class MessageUiData(
    val id: Long,
    val time: Long,
    val from: String,
    val text: String?,
    val loadImage: suspend () -> SafeResponse<Bitmap?>,
    val detail: Pair<String, String>?,
    val image: ObservableField<Bitmap?> = ObservableField<Bitmap?>()
) {
    init {
        image.set(null)
    }
}