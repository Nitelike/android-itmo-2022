package com.not_example.network_1ch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.VectorDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnAttach
import androidx.databinding.BindingAdapter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.sql.Timestamp
import java.text.SimpleDateFormat

@BindingAdapter("time")
fun setTime(textView: TextView, time: Long) {
    textView.text =
        SimpleDateFormat.getDateTimeInstance().format(Timestamp(time)).toString()
}

@BindingAdapter("bitmap")
fun setBitmap(imgView: ImageView, pic: Bitmap?) {
    imgView.setImageBitmap(pic)
}

@BindingAdapter("message_ui_data")
fun setMessageUiData(imgView: ImageView, msgUiData: MessageUiData) {
    imgView.doOnAttach {
        imgView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            if (msgUiData.image.get() == null) {
                val res = msgUiData.loadImage()
                if (!res.success()) {
                    if (res.exception() is ConnectException) {
                        imgView.setImageBitmap((imgView.context.applicationContext as MessagesApplication).brokenImage)
                    } else {
                        msgUiData.image.set((imgView.context.applicationContext as MessagesApplication).brokenImage)
                    }
                    Snackbar.make(
                        imgView,
                        res.message(),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    msgUiData.image.set(res.response())
                }
            }
        }
    }
}

@BindingAdapter("detail_activity")
fun setDetailActivity(imgView: ImageView, detail: Pair<String, String>?) {
    if (detail != null) {
        imgView.setOnClickListener {
            val intent = Intent(imgView.context, ImageDetailView::class.java)
            intent.putExtra("detailLink", detail.second)
            intent.putExtra("detailPath", detail.first)
            imgView.context.startActivity(intent)
        }
    }
}