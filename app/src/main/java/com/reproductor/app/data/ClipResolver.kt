package com.reproductor.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed interface ClipResult {
    data class Found(val ids: List<String>) : ClipResult
    data object NoKey : ClipResult
    data object Offline : ClipResult
    data class Failed(val message: String) : ClipResult
}

private class ApiException(message: String) : Exception(message)

/**
 * Busca el videoclip de una canción con la API oficial de YouTube Data v3 (clave del propio usuario)
 * y devuelve solo videos que permiten reproducirse incrustados. Los resultados se guardan en caché.
 */
class ClipResolver(context: Context, private val dao: AppDao) {
    private val ctx = context.applicationContext

    suspend fun resolve(query: String, apiKey: String?, refresh: Boolean): ClipResult = withContext(Dispatchers.IO) {
        val cacheKey = normalizeText(query)
        if (refresh) {
            dao.forgetClip(cacheKey)
        } else {
            val cached = dao.cachedClip(cacheKey)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
            if (cached.isNotEmpty()) return@withContext ClipResult.Found(cached)
        }
        if (apiKey.isNullOrBlank()) return@withContext ClipResult.NoKey
        if (!isOnline()) return@withContext ClipResult.Offline
        try {
            val ids = search(query, apiKey.trim())
            if (ids.isEmpty()) {
                ClipResult.Failed("No se encontró ningún videoclip que se pueda ver dentro de la app.")
            } else {
                dao.saveClip(ClipCacheEntity(cacheKey, ids.joinToString(","), System.currentTimeMillis()))
                ClipResult.Found(ids)
            }
        } catch (e: ApiException) {
            ClipResult.Failed(e.message ?: "Error de YouTube")
        } catch (e: Exception) {
            ClipResult.Failed("No se pudo conectar con YouTube (${e.javaClass.simpleName}).")
        }
    }

    /** Devuelve null si la clave funciona, o el motivo del error. */
    suspend fun testKey(apiKey: String): String? = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext "Sin conexión a Internet."
        try {
            search("music video", apiKey.trim())
            null
        } catch (e: ApiException) {
            e.message
        } catch (e: Exception) {
            "No se pudo conectar con YouTube (${e.javaClass.simpleName})."
        }
    }

    private fun search(query: String, key: String): List<String> {
        // Primero dentro de la categoría Música; si no hay resultados, sin categoría.
        for (musicOnly in listOf(true, false)) {
            val ids = request(query, key, musicOnly)
            if (ids.isNotEmpty()) return ids
        }
        return emptyList()
    }

    private fun request(query: String, key: String, musicOnly: Boolean): List<String> {
        val url = "https://www.googleapis.com/youtube/v3/search?part=id&type=video&videoEmbeddable=true" +
            (if (musicOnly) "&videoCategoryId=10" else "") +
            "&maxResults=5&q=" + URLEncoder.encode(query, "UTF-8") +
            "&key=" + URLEncoder.encode(key, "UTF-8")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrNull()
                throw ApiException("YouTube respondió $code: ${msg ?: "error desconocido"}")
            }
            val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
            val out = ArrayList<String>()
            for (i in 0 until items.length()) {
                val id = items.getJSONObject(i).optJSONObject("id")?.optString("videoId").orEmpty()
                if (VIDEO_ID.matches(id)) out += id
            }
            return out
        } finally {
            conn.disconnect()
        }
    }

    private fun isOnline(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object { val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$") }
}
