package com.example.webview_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webview_android.ui.components.SimpleWebView
import com.example.webview_android.ui.theme.WebviewandroidTheme

class MainActivity : ComponentActivity() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraUri?.let { uri ->
                filePathCallback?.onReceiveValue(arrayOf(uri))
                filePathCallback = null
            }
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        filePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Buscar token JWT
        val token = "testeToken"

        setContent {
            WebviewandroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WebViewScreen(
                        url = "https://poc-veiculos.wefit.com.br/stories",
                        enableJavaScript = true,
                        token = token, // Passar o token
                        modifier = Modifier.padding(innerPadding),
                        onShowFileChooser = { callback ->
                            filePathCallback = callback
                            selectImage.launch("image/*")
                        }
                    )
                }
            }
        }
    }

    // Função para buscar o token JWT
    private fun getAuthToken(): String? {

        val sharedPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        return sharedPrefs.getString("token", null)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    enableJavaScript: Boolean = false,
    token: String? = null, // Adicionar parâmetro token
    modifier: Modifier = Modifier,
    onShowFileChooser: (ValueCallback<Array<Uri>>) -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Tratar resultado da permissão se necessário
    }

    val chromeClient = remember {
        object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                onShowFileChooser(filePathCallback)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.resources?.forEach { resource ->
                    when (resource) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            // Passar o token para o SimpleWebView
            SimpleWebView(context, url, enableJavaScript, chromeClient, token)
        }
    )
}