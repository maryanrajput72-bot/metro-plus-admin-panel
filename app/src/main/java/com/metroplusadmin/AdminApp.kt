package com.metroplusadmin

import android.app.Application
import java.io.File

/**
 * Crash catcher — agar app kabhi phir band ho ("has stopped") to agli dafa
 * khulte hi Login screen pe crash ki wajah dikh jayegi, taake screenshot
 * bhej ke asal wajah pata chal sake. Andar ki file me poora stack trace
 * mehfooz rehta hai (sirf phone par, kahin upload nahi hota).
 */
class AdminApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val text = buildString {
                    append(error.javaClass.name).append(": ").append(error.message).append("\n")
                    for (frame in error.stackTrace.take(25)) append("  at ").append(frame).append("\n")
                    error.cause?.let { append("Caused by: ").append(it.javaClass.name).append(": ").append(it.message).append("\n") }
                }
                File(cacheDir, "crash-last.txt").writeText(text)
                getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
                    .edit().putString("last_crash", text).apply()
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, error)
        }
    }
}
