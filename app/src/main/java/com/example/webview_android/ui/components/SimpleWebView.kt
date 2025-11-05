package com.example.webview_android.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.webview_android.bridge.AppBridgeInterface

@SuppressLint("SetJavaScriptEnabled")
class SimpleWebView(
    context: Context,
    private val url: String,
    enableJavaScript: Boolean,
    chromeClient: WebChromeClient? = null,
    token: String? = null
) : WebView(context) {
    init {
        webViewClient = WebViewClient()
        chromeClient?.let { webChromeClient = it }

        settings.apply {
            javaScriptEnabled = enableJavaScript
            domStorageEnabled = true
        }

        // Registrar o JavaScriptInterface para o canal AppBridge ANTES de carregar a URL
        addJavascriptInterface(AppBridgeInterface(this, context), "AppBridge")

        if (token != null) {
            setupCookie(token)
        } else {
            loadUrl(url)
        }
    }

    private fun setupCookie(token: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        val cookieString = "access_token=$token; Path=/; Max-Age=31536000; SameSite=Lax; Secure"
        cookieManager.setCookie(url, cookieString)
        cookieManager.flush()

        loadUrl(url)
    }
}