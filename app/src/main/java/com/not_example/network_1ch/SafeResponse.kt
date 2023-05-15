package com.not_example.network_1ch

import android.content.res.Resources

class SafeResponse<T> {
    companion object {
        fun localizedHttpMessage(code: Int): String {
            return when (code) {
                500 -> Resources.getSystem().getString(R.string.http_500)
                in 501..599 -> Resources.getSystem().getString(R.string.http_5xx)
                404 -> Resources.getSystem().getString(R.string.http_404)
                409 -> Resources.getSystem().getString(R.string.http_409)
                413 -> Resources.getSystem().getString(R.string.http_413)
                else -> Resources.getSystem().getString(R.string.http_wtf)
            }
        }
    }

    private var response: T? = null
    private var exception: Exception? = null
    private var message: String? = null

    constructor(response: T?) {
        this.response = response
    }

    constructor(exception: Exception, message: String? = null) {
        this.exception = exception
        this.message = message
    }

    fun success(): Boolean {
        return exception == null
    }

    fun response(): T? {
        return response
    }

    fun exception(): Exception? {
        return exception
    }

    fun message(): String {
        return message ?: exception?.message.toString()
    }
}