package com.metroplusadmin

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app updater — Metro+ staff app jaisa, auto + silent.
 *
 * App open ke 5 second baad version.json check hoti hai. Naya version ho to
 * APK app ke andar hi download hoti hai (DownloadManager nahi, koi download
 * notification nahi), phir PackageInstaller session se SILENT install hoti hai:
 * self-update + REQUEST_INSTALL_PACKAGES + UPDATE_PACKAGES_WITHOUT_USER_ACTION
 * + USER_ACTION_NOT_REQUIRED = bina popup install. Koi shart poori na ho to
 * system STATUS_PENDING_USER_ACTION bhejta hai — receiver us surat me
 * installer screen khud khol deta hai (fallback, update phir bhi hogi).
 *
 * Manifest: https://raw.githubusercontent.com/maryanrajput72-bot/metro-plus-admin-panel/main/version.json
 * NOTE: repo PRIVATE hote hi ye anonymous fetch band ho jayega (Zabii ka faisla:
 * private hi rakhna, updater baad me dekhenge).
 */
object UpdateCheck {
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/maryanrajput72-bot/metro-plus-admin-panel/main/version.json"

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var cancelled = false

    // Unknown sources on karne ke liye settings gaye waqt download shuda file
    // yahan rakhi jati hai, taake wapas aate hi install khul jaye.
    var pendingInstallFile: File? = null

    // Silent session commit ho chuka ho to dobara session na bane.
    @Volatile var installInFlight = false

    fun cancelDownload() { cancelled = true }

    fun checkForUpdate(
        context: Context,
        onUpdate: (versionName: String, apkUrl: String) -> Unit,
        onNoUpdate: () -> Unit = {},
        onError: (reason: String) -> Unit = {}
    ) {
        Thread {
            // Pehle version.json (raw.githubusercontent.com) — na mile to
            // GitHub API releases/latest (alag host — ek band ho to doosra chale).
            val rawResult = tryFetchManifest()
            if (rawResult != null) {
                val (remoteCode, apkUrl, versionName) = rawResult
                if (remoteCode <= BuildConfig.VERSION_CODE || apkUrl.isBlank()) {
                    mainHandler.post { onNoUpdate() }
                } else {
                    mainHandler.post { onUpdate(versionName, apkUrl) }
                }
                return@Thread
            }
            val apiResult = tryFetchLatestRelease()
            if (apiResult != null) {
                val (remoteCode, apkUrl, versionName) = apiResult
                if (remoteCode <= BuildConfig.VERSION_CODE || apkUrl.isBlank()) {
                    mainHandler.post { onNoUpdate() }
                } else {
                    mainHandler.post { onUpdate(versionName, apkUrl) }
                }
                return@Thread
            }
            mainHandler.post { onError("manifest + GitHub API dono nahi khule (net/raw host)") }
        }.start()
    }

    // version.json parho → (versionCode, apkUrl, versionName) ya null.
    private fun tryFetchManifest(): Triple<Int, String, String>? {
        return try {
            val url = "$MANIFEST_URL?t=${System.currentTimeMillis()}"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            Triple(
                json.optInt("versionCode", 0),
                json.optString("apkUrl", ""),
                json.optString("versionName", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    // GitHub API: latest release → tag "v1.0.0-06" se code 6, APK asset ka URL.
    private fun tryFetchLatestRelease(): Triple<Int, String, String>? {
        return try {
            val url = "https://api.github.com/repos/maryanrajput72-bot/metro-plus-admin-panel/releases/latest"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val tag = json.optString("tag_name", "")
            val buildNum = tag.substringAfterLast("-", "").toIntOrNull() ?: return null
            val versionName = tag.removePrefix("v").substringBeforeLast("-") + "(${"%02d".format(buildNum)})"
            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val name = assets.getJSONObject(i).optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = assets.getJSONObject(i).optString("browser_download_url", "")
                        break
                    }
                }
            }
            Triple(buildNum, apkUrl, versionName)
        } catch (_: Exception) {
            null
        }
    }

    fun downloadUpdate(
        context: Context,
        apkUrl: String,
        onProgress: (percent: Int, downloaded: Long, total: Long) -> Unit,
        onComplete: (file: File) -> Unit,
        onError: () -> Unit
    ) {
        cancelled = false
        Thread {
            var outFile: File? = null
            try {
                val dir = File(context.cacheDir, "update").apply { mkdirs() }
                outFile = File(dir, "MetroPlusAdmin-update.apk")
                val conn = URL(apkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    mainHandler.post { onError() }; return@Thread
                }
                val total = conn.contentLengthLong
                var downloaded = 0L
                var lastPct = -2
                conn.inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            if (cancelled) { mainHandler.post { onError() }; return@Thread }
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val pct = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else -1
                            if (pct != lastPct) {
                                lastPct = pct
                                mainHandler.post { onProgress(pct, downloaded, total) }
                            }
                        }
                    }
                }
                if (cancelled) { mainHandler.post { onError() }; return@Thread }
                mainHandler.post { onProgress(100, downloaded, total) }
                mainHandler.post { onComplete(outFile) }
            } catch (_: Exception) {
                try { outFile?.delete() } catch (_: Exception) { }
                mainHandler.post { onError() }
            }
        }.start()
    }

    fun canInstall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    // SILENT install — PackageInstaller session. Session fail ho to false —
    // caller purana ACTION_VIEW rasta use kare.
    fun installUpdateSilent(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (installInFlight) return true
        return try {
            installInFlight = true
            pendingInstallFile = file
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(context.packageName)
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.openWrite("apk", 0, file.length()).use { out ->
                FileInputStream(file).use { input -> input.copyTo(out) }
            }
            val statusIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                action = UpdateInstallReceiver.ACTION_STATUS
            }
            val statusPending = PendingIntent.getBroadcast(
                context, sessionId, statusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            session.commit(statusPending.intentSender)
            session.close()
            true
        } catch (_: Exception) {
            installInFlight = false
            false
        }
    }

    fun installUpdate(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

// Silent install session ka status receiver. SUCCESS pe pending file saaf;
// PENDING_USER_ACTION pe system ki di hui installer screen kholo (fallback);
// kisi aur failure pe purana ACTION_VIEW rasta (file pendingInstallFile me mehfooz hai).
class UpdateInstallReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STATUS = "com.metroplusadmin.INSTALL_STATUS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STATUS) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        UpdateCheck.installInFlight = false
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                UpdateCheck.pendingInstallFile = null
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                try {
                    @Suppress("DEPRECATION")
                    val pending = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    pending?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (pending != null) context.startActivity(pending)
                } catch (_: Exception) {
                }
            }
            else -> {
                val file = UpdateCheck.pendingInstallFile
                if (file != null) {
                    UpdateCheck.pendingInstallFile = null
                    UpdateCheck.installUpdate(context, file)
                }
            }
        }
    }
}
