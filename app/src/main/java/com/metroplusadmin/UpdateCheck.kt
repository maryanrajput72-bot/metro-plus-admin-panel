package com.metroplusadmin

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Update check — version.json GitHub repo se (raw URL).
 * Naya versionCode mile to dialog: Download dabao to APK Downloads
 * me aayegi aur install prompt khulega. Koi link bahar nahi bhejta.
 */
object UpdateCheck {

    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/maryanrajput72-bot/metro-plus-admin-panel/main/version.json"

    fun check(activity: Activity) {
        Thread {
            try {
                val url = URL(MANIFEST_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8_000
                conn.readTimeout = 8_000
                conn.connect()
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@Thread
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val latestCode = json.optInt("versionCode", 0)
                val latestName = json.optString("versionName", "")
                val apkUrl = json.optString("apkUrl", "")
                if (latestCode <= BuildConfig.VERSION_CODE) return@Thread
                if (apkUrl.isBlank()) return@Thread
                activity.runOnUiThread {
                    showUpdateDialog(activity, latestName, apkUrl)
                }
            } catch (_: Exception) {
                // Net nahi / manifest nahi — chup chaap, app chalti rahegi.
            }
        }.start()
    }

    private fun showUpdateDialog(activity: Activity, versionName: String, apkUrl: String) {
        AlertDialog.Builder(activity)
            .setTitle("Naya update hai")
            .setMessage("Metro + Admin $versionName aagaya hai. Download karun?")
            .setPositiveButton("Download") { _, _ -> download(activity, apkUrl, versionName) }
            .setNegativeButton("Baad me", null)
            .show()
    }

    private fun download(context: Context, apkUrl: String, versionName: String) {
        val safeName = versionName.replace(Regex("[^A-Za-z0-9.()]+"), "-")
        val fileName = "MetroPlusAdmin-v$safeName.apk"
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Metro + Admin $versionName")
            .setDescription("Update download ho raha hai…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)
        context.getSharedPreferences("admin_update", Context.MODE_PRIVATE)
            .edit().putLong("download_id", id).apply()
    }
}
