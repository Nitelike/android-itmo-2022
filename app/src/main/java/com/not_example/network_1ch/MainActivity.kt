package com.not_example.network_1ch

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    // views
    private lateinit var messagesView: RecyclerView
    private lateinit var sendButton: MaterialButton
    private lateinit var attachButton: MaterialButton
    private lateinit var messageInput: EditText
    private val adapter = MessageAdapter()
    private val viewManager = LinearLayoutManager(this)

    // model
    private val model: MessagesModel by viewModels {
        MessagesModelFactory((application as MessagesApplication).repository)
    }

    // service
    private lateinit var mBinder: MessageService.LocalBinder
    private lateinit var mService: MessageService
    private var mBound = false
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            mBinder = service as MessageService.LocalBinder
            mService = mBinder.getService()
            mBinder.setClient(object : MessageService.MessageServiceClient {
                override fun processSafeResponse(res: SafeResponse<Unit>) {
                    if (!res.success()) {
                        showErrorMessage(res.message())
                    }
                }
            })
            mBound = true
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            if (mBound) {
                mBinder.setClient(null)
            }
            mBound = false
        }
    }
    private lateinit var serviceIntent: Intent

    // other
    private lateinit var mUtilities: Utilities
    private lateinit var sharedPref: SharedPreferences
    private var lastReadId = Constants.FIRST_KNOWN_ID
    private val registrationForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                try {
                    mService.sendImage(
                        contentResolver.openInputStream(it.data!!.data!!)
                    )
                } catch (e: Exception) {
                    Log.e("AttachImage", e.message.toString())
                }
            }
        }
    private val scrollDownObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            if (itemCount > 0) {
                viewManager.scrollToPosition(positionStart + itemCount - 1)
            }
            adapter.unregisterAdapterDataObserver(this)
        }
    }
    private val scrollUpObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            if (itemCount > 0) {
                viewManager.scrollToPosition(positionStart)
            }
            adapter.unregisterAdapterDataObserver(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceIntent = Intent(this, MessageService::class.java)
        startService(serviceIntent)

        mUtilities = Utilities((application as MessagesApplication).database.messageDao())
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        lastReadId = sharedPref.getLong("LastReadId", Constants.FIRST_KNOWN_ID)

        model.lrid = lastReadId
        model.cachePath = cacheDir.absolutePath
        if (model.isEmpty()) {
            lifecycleScope.launch {
                try {
                    val res = model.loadNextMessages()
                    if (!res.success()) {
                        showErrorMessage(res.message())
                        adapter.registerAdapterDataObserver(scrollDownObserver)
                    } else if (res.response()!! == 0) {
                        adapter.registerAdapterDataObserver(scrollDownObserver)
                    }
                } catch (e: IllegalStateException) { // observer is already registered
                    Log.e("FirstMessagesLoad", e.message.toString())
                }
                val res = model.loadPrevMessages()
                if (!res.success()) {
                    showErrorMessage(res.message())
                }
            }
        }
        model.messages.observe(this) { adapter.submitList(it.toList()) }

        messagesView = findViewById(R.id.messages_list)
        messagesView.layoutManager = viewManager
        messagesView.adapter = adapter

        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.message_send_btn)
        attachButton = findViewById(R.id.attach_file_btn)

        sendButton.setOnClickListener {
            if (messageInput.text.isNotEmpty()) {
                val msg = messageInput.text.toString()
                messageInput.text.clear()
                mService.sendMessage(msg)
            }
        }

        attachButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            registrationForResult.launch(intent)
        }

        messagesView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val pos = viewManager.findLastCompletelyVisibleItemPosition()
                if (pos in 0 until adapter.itemCount) lastReadId =
                    max(lastReadId, adapter.idByPosition(pos))
                lifecycleScope.launch {
                    var res: SafeResponse<Int>? = null
                    if (!recyclerView.canScrollVertically(1)) {
                        res = model.loadNextMessages()
                    } else if (!recyclerView.canScrollVertically(-1)) {
                        res = model.loadPrevMessages()
                    }
                    if (res != null && !res.success()) {
                        showErrorMessage(res.message())
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            unbindService(conn)
        }
        Log.i("LastReadId", lastReadId.toString())
        sharedPref.edit().putLong("LastReadId", lastReadId).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_panel, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val modelSize = menu!!.findItem(R.id.model_size)
        val thumbsStats = menu.findItem(R.id.thumbs_stats)
        val detailStats = menu.findItem(R.id.detail_stats)
        val dbStats = menu.findItem(R.id.count_db_entries)
        val lrid = menu.findItem(R.id.lrid)

        lifecycleScope.launch {
            modelSize.title = "${mUtilities.messagesSize(model.messages.value!!) / 1024 / 1024} Mb"
            val t1 = mUtilities.dirSize("$cacheDir/${Constants.THUMB_CACHE_DIR}")
            thumbsStats.title = "${t1.second} Mb, ${t1.first} items"
            val t2 = mUtilities.dirSize("$cacheDir/${Constants.DETAIL_CACHE_DIR}")
            detailStats.title = "${t2.second} Mb, ${t2.first} items"
            dbStats.title = "${mUtilities.countDbEntries()} rows"
        }
        lrid.title = lastReadId.toString()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear_thumbs -> {
                lifecycleScope.launch {
                    mUtilities.clearDir("$cacheDir/${Constants.THUMB_CACHE_DIR}")
                }
                true
            }
            R.id.reset_lrid -> {
                lastReadId = Constants.FIRST_KNOWN_ID
                true
            }
            R.id.clear_detail -> {
                lifecycleScope.launch {
                    mUtilities.clearDir("$cacheDir/${Constants.DETAIL_CACHE_DIR}")
                }
                true
            }
            R.id.clear_db_entries -> {
                lifecycleScope.launch {
                    mUtilities.clearDb()
                }
                true
            }
            R.id.check_id_order -> {
                val res = model.checkMessagesOrder()
                Toast.makeText(
                    baseContext,
                    if (res == 0L) "Order OK" else "Invalid order: $res",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            R.id.go_down -> {
                loadAll({ model.loadNextMessages() }, scrollDownObserver)
                true
            }
            R.id.go_up -> {
                loadAll({ model.loadPrevMessages() }, scrollUpObserver)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showErrorMessage(msg: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            msg,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun loadAll(load: suspend () -> SafeResponse<Int>, observer: RecyclerView.AdapterDataObserver) {
        lifecycleScope.launch {
            var res = load()
            try {
                adapter.registerAdapterDataObserver(observer)
            } catch (e: IllegalStateException) { // observer is already registered
                Log.e("LoadAll", e.message.toString())
            }
            while (res.success() && res.response()!! > 0) {
                res = load()
                try {
                    adapter.registerAdapterDataObserver(observer)
                } catch (e: IllegalStateException) { // observer is already registered
                    Log.e("LoadAll", e.message.toString())
                }
            }
        }
    }
}
