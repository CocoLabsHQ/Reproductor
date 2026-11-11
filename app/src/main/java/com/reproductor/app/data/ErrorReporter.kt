package com.reproductor.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * Reportes de error con consentimiento: guarda los cierres inesperados SOLO en el teléfono y,
 * cuando el usuario lo decide, abre un "issue" de GitHub con el texto ya escrito para que lo revise y lo envíe.
 * No se envía nada automáticamente.
 */
class ErrorReporter(context: Context) {
    private val ctx = context.applicationContext
    private val crashFile: File get() = File(ctx.filesDir, "last_crash.txt")

    fun deviceInfo(): String {
        val v = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
        return "Music Box $v · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /** Registra los cierres inesperados en un archivo local (y deja que Android siga con su gestión normal). */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = error.stackTraceToString().lineSequence().take(40).joinToString("\n")
                crashFile.writeText("${deviceInfo()}\nHilo: ${thread.name}\n\n$trace")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun pendingCrash(): String? =
        runCatching { crashFile.takeIf { it.exists() }?.readText() }.getOrNull()

    fun clearCrash() { runCatching { crashFile.delete() } }

    /** Abre el formulario de "nuevo issue" del repositorio con el título y los detalles ya rellenados. */
    fun issueIntent(title: String, details: String): Intent {
        val head = "**¿Qué estabas haciendo?**\n(escríbelo aquí)\n\n**Detalles técnicos**\n```\n"
        val tail = "\n```\n\n**Dispositivo:** ${deviceInfo()}"
        var d = details
        var trimmed = false
        // Las direcciones web largas fallan: se recorta el texto para que quepa.
        while (Uri.encode(head + d + tail).length > 5500 && d.length > 200) {
            d = d.dropLast(200)
            trimmed = true
        }
        if (trimmed) d += "\n…(recortado)"
        val uri = Uri.Builder()
            .scheme("https").authority("github.com")
            .appendPath("CocoLabsHQ").appendPath("Reproductor").appendPath("issues").appendPath("new")
            .appendQueryParameter("title", title)
            .appendQueryParameter("body", head + d + tail)
            .build()
        return Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
