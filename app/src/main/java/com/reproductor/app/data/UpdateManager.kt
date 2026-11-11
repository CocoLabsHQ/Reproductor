package com.reproductor.app.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class UpdateInfo(
    val version: String,
    val notes: String,
    val sizeBytes: Long,
    val url: String,
    val fileName: String,
)

data class DownloadProgress(val status: Int, val downloaded: Long, val total: Long) {
    val percent: Int get() = if (total > 0) (downloaded * 100 / total).toInt().coerceIn(0, 100) else 0
}

sealed interface UpdateUi {
    data object Hidden : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateUi
    data class NeedsPermission(val info: UpdateInfo) : UpdateUi
    data class Error(val message: String) : UpdateUi
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "tamaño desconocido"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb) else "${bytes / 1024} KB"
}

/**
 * Busca versiones nuevas en las Releases de GitHub, descarga el APK con el DownloadManager del sistema
 * y lanza el instalador. Es la única función (junto con los videoclips) que usa Internet.
 */
class UpdateManager(context: Context) {
    private val ctx = context.applicationContext

    fun currentVersion(): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) { runCatching { fetch() }.getOrNull() }

    private fun fetch(): UpdateInfo? {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val remote = json.getString("tag_name").removePrefix("v")
            if (!isNewer(remote, currentVersion())) return null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.getString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    return UpdateInfo(
                        version = remote,
                        notes = json.optString("body", "").trim(),
                        sizeBytes = a.optLong("size", 0L),
                        url = a.getString("browser_download_url"),
                        fileName = name,
                    )
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split('.').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun apkFile(info: UpdateInfo) =
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), info.fileName)

    private fun dm() = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueue(info: UpdateInfo): Long {
        apkFile(info).delete()
        val req = DownloadManager.Request(Uri.parse(info.url))
            .setTitle("Reproductor ${info.version}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, info.fileName)
        return dm().enqueue(req)
    }

    fun progress(id: Long): DownloadProgress {
        dm().query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (c.moveToFirst()) {
                return DownloadProgress(
                    status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                )
            }
        }
        return DownloadProgress(DownloadManager.STATUS_FAILED, 0, 0)
    }

    fun canInstall(): Boolean = Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    fun permissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installIntent(info: UpdateInfo): Intent {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apkFile(info))
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Notas de "Novedades" incluidas dentro de esta versión de la app (funcionan sin conexión). */
    fun bundledNotes(): String =
        runCatching { ctx.assets.open("whatsnew.txt").bufferedReader().use { it.readText() }.trim() }.getOrDefault("")

    private companion object { const val REPO = "CocoLabsHQ/Reproductor" }
}
