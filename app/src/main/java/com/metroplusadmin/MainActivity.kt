package com.metroplusadmin

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

        webView = WebView(this)
        setContentView(webView)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/panel/index.html")

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
