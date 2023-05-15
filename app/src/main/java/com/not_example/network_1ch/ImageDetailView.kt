package com.not_example.network_1ch

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.not_example.network_1ch.databinding.ActivityImageDetailViewBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException

class ImageDetailView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)

        val model: ImageDetailModel by viewModels {
            ImageDetailModelFactory((application as MessagesApplication).repository)
        }

        val binding = ActivityImageDetailViewBinding.inflate(layoutInflater)
        binding.model = model

        try {
            model.imageLink = intent.getStringExtra("detailLink")!!
            lifecycleScope.launch {
                val res = model.setImage(
                    intent.getStringExtra("detailPath")!!,
                    intent.getStringExtra("detailLink")!!
                )
                if (!res.success()) {
                    if (res.exception() is ConnectException || res.exception() is HttpException) {
                        binding.imageDetailView.setImageBitmap((application as MessagesApplication).brokenImage)
                    } else {
                        model.setImage((application as MessagesApplication).brokenImage)
                    }
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        res.message(),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ImageDetailView: ", e.message.toString())
        }

        setContentView(binding.root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

