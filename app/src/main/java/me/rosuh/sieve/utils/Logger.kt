package me.rosuh.sieve.utils

object Logger {
    fun d(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
    }

    fun e(tag: String, msg: String) {
        android.util.Log.e(tag, msg)
    }

    fun i(tag: String, msg: String) {
        android.util.Log.i(tag, msg)
    }

    fun v(tag: String, msg: String) {
        android.util.Log.v(tag, msg)
    }

    fun w(tag: String, msg: String) {
        android.util.Log.w(tag, msg)
    }

    fun wtf(tag: String, msg: String) {
        android.util.Log.wtf(tag, msg)
    }
}