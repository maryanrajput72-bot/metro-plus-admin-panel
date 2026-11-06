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

    private fun runUpdateCheck() {
        UpdateCheck.checkForUpdate(
            context = this,
            onUpdate = { versionName, apkUrl ->
                Toast.makeText(this, "Update $versionName download ho raha hai…", Toast.LENGTH_SHORT).show()
                UpdateCheck.downloadUpdate(
                    context = this,
                    apkUrl = apkUrl,
                    onProgress = { _, _, _ -> },
                    onComplete = { file ->
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
                    onError = { }
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
