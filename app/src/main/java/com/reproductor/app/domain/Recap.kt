package com.reproductor.app.domain

import com.reproductor.app.data.AppDao
import com.reproductor.app.data.ArtistStat
import com.reproductor.app.data.SongStat
import com.reproductor.app.data.Totals
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class MonthTop(val label: String, val song: SongStat?)

data class Recap(
    val title: String,
    val isYear: Boolean,
    val totals: Totals,
    val topSongs: List<SongStat>,
    val topArtists: List<ArtistStat>,
    val peakHour: Int?,
    val monthly: List<MonthTop>,
)

data class YearStatus(val hasData: Boolean = false, val unlocked: Boolean = false, val daysLeft: Int = 365)
data class MonthRow(val ym: YearMonth, val label: String, val minutes: Long)

/** Construye los resúmenes (mensual y anual) a partir del registro de escucha. */
class RecapRepository(private val dao: AppDao) {
    private fun startOf(ym: YearMonth): Long =
        ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun label(ym: YearMonth, withYear: Boolean = true): String {
        val name = ym.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
        return if (withYear) "$name ${ym.year}" else name
    }

    suspend fun month(ym: YearMonth): Recap {
        val from = startOf(ym)
        val to = startOf(ym.plusMonths(1))
        return Recap(
            title = label(ym),
            isYear = false,
            totals = dao.totals(from, to),
            topSongs = dao.topSongs(from, to, 5),
            topArtists = dao.topArtists(from, to, 5),
            peakHour = dao.peakHour(from, to),
            monthly = emptyList(),
        )
    }

    /** Últimos 365 días, con la canción más escuchada de cada mes. */
    suspend fun year(): Recap {
        val now = System.currentTimeMillis()
        val from = now - DAY_MS * 365
        val to = now + DAY_MS
        val current = YearMonth.now()
        val monthly = (11 downTo 0).map { i ->
            val ym = current.minusMonths(i.toLong())
            MonthTop(label(ym, withYear = false), dao.topSongs(startOf(ym), startOf(ym.plusMonths(1)), 1).firstOrNull())
        }
        return Recap(
            title = "Tu año en música",
            isYear = true,
            totals = dao.totals(from, to),
            topSongs = dao.topSongs(from, to, 5),
            topArtists = dao.topArtists(from, to, 5),
            peakHour = dao.peakHour(from, to),
            monthly = monthly,
        )
    }

    suspend fun yearStatus(): YearStatus {
        val first = dao.firstPlay() ?: return YearStatus()
        val days = ((System.currentTimeMillis() - first) / DAY_MS).toInt()
        return YearStatus(hasData = true, unlocked = days >= 365, daysLeft = (365 - days).coerceAtLeast(0))
    }

    suspend fun monthRows(): List<MonthRow> =
        dao.months().mapNotNull { s -> runCatching { YearMonth.parse(s) }.getOrNull() }.map { ym ->
            MonthRow(ym, label(ym), dao.totals(startOf(ym), startOf(ym.plusMonths(1))).ms / 60_000)
        }

    private companion object { const val DAY_MS = 24L * 60 * 60 * 1000 }
}
