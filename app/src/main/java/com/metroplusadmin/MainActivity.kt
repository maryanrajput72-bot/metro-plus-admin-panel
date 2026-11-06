package com.metroplusadmin

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Metro + Admin — WebView app.
 * Panel app ke andar hi hai (assets/panel/index.html) — browser ya
 * website ki zaroorat nahi, offline bhi khulta hai. Wahi panel
 * repo ke panel/ folder me website ki tarah bhi hai — ek hi code.
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
        Handler(Looper.getMainLooper()).postDelayed(
            { UpdateCheck.check(this) },
            5_000L
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
