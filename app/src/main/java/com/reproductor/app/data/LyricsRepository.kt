package com.reproductor.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.reproductor.app.domain.LrcParser
import com.reproductor.app.domain.LyricsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Busca letras locales (.lrc / .txt):
 *  1. junto al archivo de audio (funciona en Android 10 o anterior),
 *  2. en la carpeta de letras que el usuario eligió con el selector de carpetas (SAF).
 */
class LyricsRepository(context: Context) {
    private val appContext = context.applicationContext
    private var indexedTree: String? = null
    private var index: Map<String, Uri> = emptyMap()

    @Synchronized
    fun invalidate() { indexedTree = null; index = emptyMap() }

    suspend fun load(track: Track, treeUri: String?): LyricsState = withContext(Dispatchers.IO) {
        runCatching { find(track, treeUri) }.getOrDefault(LyricsState.None)
    }

    private fun find(track: Track, treeUri: String?): LyricsState {
        // 1) Junto al archivo de audio
        for (ext in listOf("lrc", "txt")) {
            val f = File(track.path.substringBeforeLast('.', track.path) + "." + ext)
            if (f.isFile && f.canRead() && f.length() < MAX_BYTES) {
                val parsed = LrcParser.parse(decode(f.readBytes()))
                if (parsed !is LyricsState.None) return parsed
            }
        }
        // 2) Carpeta de letras elegida por el usuario
        if (treeUri != null) {
            val idx = ensureIndex(treeUri)
            val base = track.fileName.substringBeforeLast('.')
            val names = listOf(base, "${track.artist} - ${track.title}", track.title).map { it.lowercase() }
            for (ext in listOf("lrc", "txt")) {
                for (n in names) {
                    val uri = idx["$n.$ext"] ?: continue
                    val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
                    if (bytes.size > MAX_BYTES) continue
                    val parsed = LrcParser.parse(decode(bytes))
                    if (parsed !is LyricsState.None) return parsed
                }
            }
        }
        return LyricsState.None
    }

    @Synchronized
    private fun ensureIndex(treeUri: String): Map<String, Uri> {
        if (indexedTree == treeUri) return index
        val tree = Uri.parse(treeUri)
        val out = HashMap<String, Uri>()
        fun walk(docId: String, depth: Int) {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            appContext.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (depth < 4) walk(id, depth + 1)
                    } else {
                        val lower = name.lowercase()
                        if (lower.endsWith(".lrc") || lower.endsWith(".txt")) {
                            out.putIfAbsent(lower, DocumentsContract.buildDocumentUriUsingTree(tree, id))
                        }
                    }
                }
            }
        }
        runCatching { walk(DocumentsContract.getTreeDocumentId(tree), 0) }
        indexedTree = treeUri
        index = out
        return out
    }

    private fun decode(bytes: ByteArray): String {
        val utf8 = String(bytes, Charsets.UTF_8)
        return if ('\uFFFD' in utf8) String(bytes, Charsets.ISO_8859_1) else utf8
    }

    private companion object { const val MAX_BYTES = 512 * 1024 }
}
