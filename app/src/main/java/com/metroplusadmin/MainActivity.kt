package com.metroplusadmin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * Metro + Admin — WebView app.
 * Panel app ke andar hi hai (assets/panel/index.html) — browser ya
 * website ki zaroorat nahi, offline bhi khulta hai. Wahi panel
 * repo ke panel/ folder me website ki tarah bhi hai — ek hi code.
 *
 * Yahan sirf login ke BAAD aate hain (LoginActivity se). Update check
 * Metro+ jaisa: 5 second baad, auto + silent — naya version ho to APK
 * khud download + silent install, koi tap nahi.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            webView = WebView(this)
            setContentView(webView)

            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webView.webViewClient = WebViewClient()
            // Settings tab ka bridge — panel se naam/password badlo.
            webView.addJavascriptInterface(SettingsBridge(this), "AdminSettings")
            webView.loadUrl("file:///android_asset/panel/index.html")
        } catch (e: Exception) {
            // WebView na khule to app band nahi hogi — wajah screen par aayegi.
            val errorView = android.widget.TextView(this).apply {
                text = "Panel nahi khula:\n${e.javaClass.simpleName}: ${e.message}"
                textSize = 16f
                setPadding(48, 48, 48, 48)
            }
            setContentView(errorView)
            return
        }

        // Zabii ka sticky rule: app open pe 5 second baad update check LAZMI.
        Handler(Looper.getMainLooper()).postDelayed({ runUpdateCheck() }, 5_000L)
    }

    private var updateDialog: android.app.AlertDialog? = null
    private var updateBar: android.widget.ProgressBar? = null
    private var updateText: android.widget.TextView? = null

    private fun showUpdateProgress(versionName: String) {
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        updateText = android.widget.TextView(this).apply {
            text = "Update $versionName download ho rahi hai… 0%"
            textSize = 15f
        }
        box.addView(updateText)
        updateBar = android.widget.ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#0F766E")
            )
        }
        box.addView(updateBar, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 24 })
        updateDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Metro + Admin update")
            .setView(box)
            .setCancelable(false)
            .create()
        updateDialog?.show()
    }

    private fun hideUpdateProgress() {
        try { updateDialog?.dismiss() } catch (_: Exception) { }
        updateDialog = null
        updateBar = null
        updateText = null
    }

    private fun mb(bytes: Long): String =
        String.format("%.1f MB", bytes / 1024f / 1024f)

    private fun runUpdateCheck() {
        UpdateCheck.checkForUpdate(
            context = this,
            onUpdate = { versionName, apkUrl ->
                showUpdateProgress(versionName)
                UpdateCheck.downloadUpdate(
                    context = this,
                    apkUrl = apkUrl,
                    onProgress = { percent, downloaded, total ->
                        updateBar?.progress = percent
                        updateText?.text = if (total > 0) {
                            "Update $versionName download ho rahi hai… $percent% (${mb(downloaded)} / ${mb(total)})"
                        } else {
                            "Update $versionName download ho rahi hai… $percent%"
                        }
                    },
                    onComplete = { file ->
                        hideUpdateProgress()
                        if (!UpdateCheck.installUpdateSilent(this, file)) {
                            // Silent na ho sake to installer screen (phir bhi update hogi).
                            if (UpdateCheck.canInstall(this)) {
                                UpdateCheck.installUpdate(this, file)
                            } else {
                                UpdateCheck.pendingInstallFile = file
                                Toast.makeText(
                                    this,
                                    "Update ke liye 'Install unknown apps' on karo",
                                    Toast.LENGTH_LONG
                                ).show()
                                UpdateCheck.openUnknownSourcesSettings(this)
                            }
                        }
                    },
                    onError = {
                        hideUpdateProgress()
                        Toast.makeText(
                            this,
                            "Update download nahi hui — net check karke dobara kholo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        )
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

/**
 * Panel ke Settings tab ka bridge — naam/password badlo.
 * Password yahan kabhi plain save nahi hota: Auth sirf salt + SHA-256 hash
 * rakhta hai. Result: "ok" / "wrong_current" / "empty" / "short".
 */
class SettingsBridge(private val context: Context) {

    @JavascriptInterface
    fun getAdminName(): String {
        val prefs = context.getSharedPreferences(Auth.PREFS, Context.MODE_PRIVATE)
        return Auth.storedName(prefs)
    }

    @JavascriptInterface
    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    @JavascriptInterface
    fun changeCredentials(currentPassword: String, newName: String, newPassword: String): String {
        val prefs = context.getSharedPreferences(Auth.PREFS, Context.MODE_PRIVATE)
        return Auth.changeCredentials(prefs, currentPassword, newName, newPassword)
    }
}
