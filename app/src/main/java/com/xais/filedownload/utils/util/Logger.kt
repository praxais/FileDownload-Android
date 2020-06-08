package com.xais.filedownload.utils.util

import android.util.Log
import com.downloader.BuildConfig

/**
 * Created by prajwal on 6/8/20.
 */

object Logger {
    fun d(tag: String?, message: String?) {
        if (BuildConfig.DEBUG && !tag.isNullOrBlank() && !message.isNullOrBlank()) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String?, message: String?) {
        if (BuildConfig.DEBUG && !tag.isNullOrBlank() && !message.isNullOrBlank()) {
            Log.i(tag, message)
        }
    }

    fun e(tag: String?, message: String?) {
        if (BuildConfig.DEBUG && !tag.isNullOrBlank() && !message.isNullOrBlank()) {
            Log.e(tag, message)
        }
    }
}