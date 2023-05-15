package com.not_example.network_1ch

import android.app.Application
import android.graphics.drawable.VectorDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MessagesApplication : Application() {
    val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://213.189.221.170:8008/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }
    val serverAPI by lazy { retrofit.create(ServerAPI::class.java) }
    val database by lazy { MessagesDatabase.getDatabase(this) }
    val repository by lazy { DefaultMessageRepository(database.messageDao(), serverAPI) }
    val brokenImage by lazy {
        (ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_baseline_broken_image,
            null
        ) as VectorDrawable).toBitmap()
    }
    val brokenImageBlack by lazy {
        (ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_baseline_broken_image_black,
            null
        ) as VectorDrawable).toBitmap()
    }
}