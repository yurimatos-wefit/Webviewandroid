package com.example.webview_android.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Handler responsável por gerenciar o compartilhamento de imagens
 * recebidas do WebView para apps nativos do Android
 */
class ImageShareHandler(private val context: Context) {

    companion object {
        private const val TAG = "ImageShareHandler"
        private const val AUTHORITY = "com.example.webview_android.fileprovider"
        private const val SHARED_IMAGE_NAME = "shared_image"
    }

    /**
     * Compartilha uma imagem codificada em Base64
     *
     * @param base64Image String da imagem em formato base64 (com ou sem prefixo data:image/...)
     * @param mimeType Tipo MIME da imagem (padrão: image/png)
     * @param metadata Metadados opcionais sobre a imagem
     * @return true se o compartilhamento foi iniciado com sucesso
     */
    fun shareImageFromBase64(
        base64Image: String,
        mimeType: String = "image/png",
        metadata: Map<String, Any>? = null
    ): Boolean {
        return try {
            // Remover prefixo data:image/... se existir
            val cleanBase64 = base64Image.substringAfter(",")

            // Decodificar Base64 para bitmap
            val bitmap = decodeBase64ToBitmap(cleanBase64)

            // Salvar temporariamente no cache
            val imageFile = saveBitmapToCache(bitmap, mimeType)

            // Criar URI usando FileProvider
            val imageUri = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                imageFile
            )

            // Abrir share sheet do Android
            openShareSheet(imageUri, mimeType)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao compartilhar imagem", e)
            false
        }
    }

    /**
     * Decodifica string Base64 para Bitmap
     */
    private fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            ?: throw IllegalArgumentException("Falha ao decodificar imagem")
    }

    /**
     * Salva o bitmap no diretório de cache da aplicação
     */
    private fun saveBitmapToCache(bitmap: Bitmap, mimeType: String): File {
        // Determinar extensão do arquivo baseado no MIME type
        val extension = when (mimeType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "png"
        }

        // Determinar formato de compressão
        val format = when (mimeType) {
            "image/jpeg", "image/jpg" -> Bitmap.CompressFormat.JPEG
            "image/webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.PNG
        }

        // Criar arquivo no cache
        val cacheDir = File(context.cacheDir, "shared_images").apply {
            if (!exists()) mkdirs()
        }

        val imageFile = File(cacheDir, "$SHARED_IMAGE_NAME.$extension")

        // Salvar bitmap
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(format, 100, outputStream)
        }

        Log.d(TAG, "Imagem salva em: ${imageFile.absolutePath}")
        return imageFile
    }

    /**
     * Abre o share sheet nativo do Android
     */
    private fun openShareSheet(imageUri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Compartilhar imagem").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
        Log.d(TAG, "Share sheet aberto com sucesso")
    }

    /**
     * Abre a imagem em um app visualizador de imagens nativo
     */
    private fun openImageViewerIntent(imageUri: Uri, mimeType: String) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(imageUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Verificar se há apps que podem abrir imagens
        val chooserIntent = Intent.createChooser(viewIntent, "Visualizar imagem").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
        Log.d(TAG, "Visualizador de imagem aberto com sucesso")
    }

    /**
     * Limpa imagens antigas do cache (chamada opcional para limpeza)
     */
    fun cleanupOldImages() {
        try {
            val cacheDir = File(context.cacheDir, "shared_images")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                Log.d(TAG, "Cache de imagens limpo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar cache", e)
        }
    }
}
