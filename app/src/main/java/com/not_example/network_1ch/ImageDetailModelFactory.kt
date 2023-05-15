package com.not_example.network_1ch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ImageDetailModelFactory(private val repo: MessageRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageDetailModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImageDetailModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}