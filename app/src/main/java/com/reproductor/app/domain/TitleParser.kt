package com.reproductor.app.domain

import com.reproductor.app.data.TrackOverrideEntity
import com.reproductor.app.data.Track
import com.reproductor.app.data.UNKNOWN_ARTIST
import com.reproductor.app.data.normalizeText

data class Enriched(val tracks: List<Track>, val local: Int)

/** Un nombre con forma "A — B" cuyo autor aún no se sabe cuál de los dos lados es. */
data class Candidate(val trackId: Long, val first: String, val second: String)

/** Resultado de decidir el orden: [firstIsArtist] = "Autor — Título"; false = "Título — Autor". */
data class Parsed(val artist: String, val title: String, val firstIsArtist: Boolean)

/**
 * Detecta autor y título cuando el archivo no trae autor en sus etiquetas y el nombre es "Autor — Título".
 * Para decidir qué lado es el autor usa la propia biblioteca: los autores que ya aparecen en etiquetas
 * y los nombres que se repiten en un lado u otro. Si no hay pistas, se asume "Autor — Título".
 */
object TitleParser {
    private val separators = Regex("""\s+[-–—―‒|]\s+|\s*[–—―‒]\s*""")
    private val noise = Regex(
        """\s*[\(\[]\s*(official|lyrics?|audio|video|music video|hd|hq|4k|remaster(ed)?|visualizer|letra)[^\)\]]*[\)\]]""",
        RegexOption.IGNORE_CASE,
    )
    private val leadingNumber = Regex("""^\s*\d{1,3}\s*[-–—._)]\s*""")

    private fun norm(s: String) = normalizeText(s).trim()

    fun clean(raw: String): String {
        var s = raw.trim()
        if ('_' in s && ' ' !in s) s = s.replace('_', ' ')
        s = noise.replace(s, "")
        s = leadingNumber.replace(s, "")
        return s.trim()
    }

    private fun plausibleName(s: String): Boolean =
        s.length in 2..40 && s.split(' ').size <= 6 && !s.all { it.isDigit() }

    /** Separa por el primer guion largo (— – ―) o " - " / " | " con espacios. */
    fun split(raw: String): Pair<String, String>? {
        val s = clean(raw)
        val m = separators.find(s) ?: return null
        val a = s.substring(0, m.range.first).trim()
        val b = s.substring(m.range.last + 1).trim()
        if (a.length < 2 || b.length < 2) return null
        return a to b
    }

    fun candidates(tracks: List<Track>): List<Candidate> = tracks.mapNotNull { t ->
        if (t.artist != UNKNOWN_ARTIST) return@mapNotNull null
        val sp = split(t.title.ifBlank { t.fileName.substringBeforeLast('.') }) ?: return@mapNotNull null
        if (!plausibleName(sp.first) && !plausibleName(sp.second)) null else Candidate(t.id, sp.first, sp.second)
    }

    fun knownArtists(tracks: List<Track>): Set<String> =
        tracks.asSequence().filter { it.artist != UNKNOWN_ARTIST }.map { norm(it.artist) }.toHashSet()

    fun decide(cands: List<Candidate>, known: Set<String>): Map<Long, Parsed> {
        val left = HashMap<String, Int>()
        val right = HashMap<String, Int>()
        for (c in cands) {
            val a = norm(c.first)
            val b = norm(c.second)
            left[a] = (left[a] ?: 0) + 1
            right[b] = (right[b] ?: 0) + 1
        }
        val out = HashMap<Long, Parsed>()
        for (c in cands) {
            val a = norm(c.first)
            val b = norm(c.second)
            // Puntos de "es un autor": sale en etiquetas (+3), se repite a la izquierda, o aparece como título en otros.
            val scoreFirst = (if (a in known) 3 else 0) + ((left[a] ?: 1) - 1) - (right[a] ?: 0)
            val scoreSecond = (if (b in known) 3 else 0) + (left[b] ?: 0) - ((right[b] ?: 1) - 1)
            val firstIsArtist = scoreSecond <= scoreFirst // en empate: "Autor — Título"
            out[c.trackId] = if (firstIsArtist) Parsed(c.first, c.second, true) else Parsed(c.second, c.first, false)
        }
        return out
    }

    /** Aplica las correcciones guardadas (manuales / por Internet) y, si [smart], la detección local. */
    fun enrich(tracks: List<Track>, smart: Boolean, overrides: List<TrackOverrideEntity>): Enriched {
        val ov = overrides.associateBy { it.trackId }
        val local: Map<Long, Parsed> = if (smart) {
            decide(candidates(tracks.filter { it.id !in ov }), knownArtists(tracks))
        } else emptyMap()
        if (ov.isEmpty() && local.isEmpty()) return Enriched(tracks, 0)
        val result = tracks.map { t ->
            val o = ov[t.id]
            val p = local[t.id]
            when {
                o != null -> t.copy(artist = o.artist, title = o.title, genre = o.genre ?: t.genre)
                p != null -> t.copy(artist = p.artist, title = p.title)
                else -> t
            }
        }
        return Enriched(result, local.size)
    }
}
