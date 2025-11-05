package com.example.webview_android.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import org.json.JSONObject

class AppBridgeInterface(
    private val webView: WebView,
    private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val jsonMessage = JSONObject(message)
            val type = jsonMessage.optString("type")

            when (type) {
                "SESSION_EXPIRED", "TOKEN_REFRESH_REQUEST" -> {
                    // Mostrar feedback visual nativo
                    showToast("📩 Notificação recebida: $type")
                    handleTokenRefresh(type)
                }
                else -> {
                    // Mostrar qualquer outra mensagem recebida
                    showToast("📨 Mensagem recebida do WebView: $type")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AppBridge", "Erro ao processar mensagem", e)
            showToast("❌ Erro ao processar mensagem: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleTokenRefresh(type: String) {
        // Solicitar refresh token (hardcoded no debug)
        val newToken = requestRefreshToken()

        // Atualizar token na WebView
        updateTokenInWebView(newToken)

        // Notificar a WebView que o token foi atualizado
        notifyFrontend(success = true, newToken = newToken)

        // Mostrar feedback de sucesso (APENAS PARA DEBUG)
        showToast("✅ Token atualizado com sucesso!")
    }

    private fun requestRefreshToken(): String {
        // TODO: Implementar lógica real de refresh token
        return "new_token_${System.currentTimeMillis()}"
    }

    private fun updateTokenInWebView(token: String) {
        mainHandler.post {
            val script = """
                (function() {
                    try {
                        // Definir o token como cookie
                        // path=/ garante que o cookie é acessível em todo o site
                        // SameSite=Lax fornece proteção CSRF
                        document.cookie = 'access_token=$token; path=/; SameSite=Lax';
                        return 'success';
                    } catch(e) {
                        console.error('Erro ao atualizar token:', e);
                        return 'error: ' + e.message;
                    }
                })();
            """.trimIndent()

            webView.evaluateJavascript(script) { result ->
                Log.d("AppBridge", "Token cookie update result: $result")
            }
        }
    }

    private fun notifyFrontend(success: Boolean, newToken: String? = null, error: String? = null) {
        mainHandler.post {
            webView.evaluateJavascript("window.onSessionRefreshed && window.onSessionRefreshed();", null)
        }
    }
}
