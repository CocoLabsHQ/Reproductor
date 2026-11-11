package com.reproductor.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.reproductor.app.data.Track

/** Selección múltiple de canciones: mantén pulsada una para empezar y toca las demás. */
@Stable
class SelectionState {
    /** Canciones elegidas, en el orden en que se eligieron. */
    var items by mutableStateOf<Map<Long, Track>>(emptyMap())
        private set

    /** Lista que muestra la pantalla actual (para "Todo"). La registra cada pantalla. */
    var page: List<Track> = emptyList()

    /** Acción extra según la pantalla (por ejemplo "Quitar" dentro de una playlist). */
    var extra by mutableStateOf<Pair<String, (List<Track>) -> Unit>?>(null)

    val active: Boolean get() = items.isNotEmpty()
    val count: Int get() = items.size
    val tracks: List<Track> get() = items.values.toList()
    val allSelected: Boolean get() = page.isNotEmpty() && page.all { it.id in items }

    fun isSelected(id: Long): Boolean = id in items

    fun toggle(track: Track) {
        val m = LinkedHashMap(items)
        if (m.remove(track.id) == null) m[track.id] = track
        items = m
    }

    fun togglePage() {
        val m = LinkedHashMap(items)
        if (allSelected) page.forEach { m.remove(it.id) } else page.forEach { m.putIfAbsent(it.id, it) }
        items = m
    }

    fun clear() { items = emptyMap() }
}

val LocalSelection = staticCompositionLocalOf<SelectionState> { error("SelectionState no proporcionado") }

@Composable
fun SelectionBar(
    sel: SelectionState,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onHide: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Column(Modifier.navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { sel.clear() }) { Icon(Icons.Rounded.Close, "Cancelar selección") }
                Text(
                    if (sel.count == 1) "1 seleccionada" else "${sel.count} seleccionadas",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { sel.togglePage() }) { Text(if (sel.allSelected) "Ninguna" else "Todo") }
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BarAction(Icons.Rounded.PlayArrow, "Reproducir", onPlay)
                BarAction(Icons.AutoMirrored.Rounded.QueueMusic, "Cola", onQueue)
                BarAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "Playlist", onPlaylist)
                val extra = sel.extra
                if (extra != null) BarAction(Icons.Rounded.Delete, extra.first) { extra.second(sel.tracks); sel.clear() }
                BarAction(Icons.Rounded.VisibilityOff, "Ocultar", onHide)
            }
        }
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
