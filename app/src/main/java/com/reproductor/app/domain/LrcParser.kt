package com.reproductor.app.domain

data class LrcLine(val timeMs: Long, val text: String)

sealed interface LyricsState {
    data object None : LyricsState
    data class Plain(val text: String) : LyricsState
    data class Synced(val lines: List<LrcLine>) : LyricsState
}

/** Interpreta archivos LRC (con marcas de tiempo) y TXT (texto plano). */
object LrcParser {
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""\[offset:\s*([+-]?\d+)]""", RegexOption.IGNORE_CASE)
    private val metaLine = Regex("""^\[[A-Za-z]+:.*]$""")

    fun parse(raw: String): LyricsState {
        val text = raw.removePrefix("\uFEFF")
        val offset = offsetTag.find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val lines = ArrayList<LrcLine>()
        for (line in text.lineSequence()) {
            val matches = timeTag.findAll(line).toList()
            if (matches.isEmpty()) continue
            val content = line.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val millis = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                lines += LrcLine((minutes * 60_000 + seconds * 1000 + millis - offset).coerceAtLeast(0), content)
            }
        }
        if (lines.isNotEmpty()) return LyricsState.Synced(lines.sortedBy { it.timeMs })
        val plain = text.lineSequence().filterNot { metaLine.matches(it.trim()) }.joinToString("\n").trim()
        return if (plain.isBlank()) LyricsState.None else LyricsState.Plain(plain)
    }

    /** Índice de la línea activa para una posición dada (-1 si aún no empieza). */
    fun activeIndex(lines: List<LrcLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.lastIndex
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) { ans = mid; lo = mid + 1 } else hi = mid - 1
        }
        return ans
    }
}
