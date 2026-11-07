package com.reproductor.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.data.PlaylistUi
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.EmptyState
import com.reproductor.app.ui.components.PlayShuffleButtons
import com.reproductor.app.ui.components.ReorderableList
import com.reproductor.app.ui.components.TrackArt
import com.reproductor.app.ui.components.TrackRow

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name); onDismiss() }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun PlaylistsScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var toRename by remember { mutableStateOf<PlaylistUi?>(null) }
    var toDelete by remember { mutableStateOf<PlaylistUi?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Playlists", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
            )
            if (playlists.isEmpty()) {
                EmptyState("Aún no tienes playlists. Toca + para crear la primera.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(playlists, key = { it.id }) { p ->
                        var menu by remember { mutableStateOf(false) }
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TrackArt(p.tracks.firstOrNull(), Modifier.size(56.dp), px = 256)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${p.tracks.size} canciones", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Opciones") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Reproducir") },
                                        onClick = { menu = false; vm.play(p.tracks, 0) },
                                    )
                                    DropdownMenuItem(text = { Text("Cambiar nombre") }, onClick = { menu = false; toRename = p })
                                    DropdownMenuItem(text = { Text("Eliminar playlist") }, onClick = { menu = false; toDelete = p })
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showCreate = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) { Icon(Icons.Rounded.Add, "Nueva playlist") }
    }

    if (showCreate) {
        NameDialog("Nueva playlist", "", "Crear", { showCreate = false }) { vm.createPlaylist(it) }
    }
    toRename?.let { p ->
        NameDialog("Cambiar nombre", p.name, "Guardar", { toRename = null }) { vm.renamePlaylist(p.id, it) }
    }
    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Eliminar «${p.name}»?") },
            text = { Text("Solo se elimina la lista. Los archivos de música no se borran.") },
            confirmButton = { TextButton(onClick = { vm.deletePlaylist(p.id); toDelete = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun PlaylistDetailScreen(vm: MainViewModel, onBack: () -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == vm.openPlaylistId }
    val source = playlist?.tracks.orEmpty()
    // Copia local para reordenar con fluidez; se guarda en Room al soltar.
    val items = remember(source) { source.toMutableStateList() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text(playlist?.name ?: "Playlist", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.headlineMedium)
                Text("${items.size} canciones · mantén y arrastra ≡ para reordenar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (playlist == null || items.isEmpty()) {
            EmptyState("Esta playlist está vacía. Añade canciones desde el menú ⋮ de cualquier canción.")
        } else {
            PlayShuffleButtons(
                onPlay = { vm.play(items.toList(), 0) },
                onShuffle = { vm.shufflePlay(items.toList()) },
            )
            ReorderableList(
                items = items,
                key = { it.id },
                onCommit = { _, _ -> vm.savePlaylistOrder(playlist.id, items.map { it.id }) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) { t, handle, dragging ->
                TrackRow(
                    track = t,
                    isCurrent = state.currentId == t.id,
                    onClick = { vm.play(items.toList(), items.indexOf(t)) },
                    modifier = if (dragging) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier,
                    leading = {
                        Icon(
                            Icons.Rounded.DragHandle, "Reordenar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = handle.size(32.dp).padding(end = 8.dp),
                        )
                    },
                    extraMenu = listOf("Quitar de la playlist" to { vm.removeFromPlaylist(playlist.id, t) }),
                )
            }
        }
    }
}

@Composable
fun CollectionScreen(vm: MainViewModel, onBack: () -> Unit) {
    val collection = vm.collection
    val map by vm.trackMap.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text(collection?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.headlineMedium)
                Text(
                    collection?.subtitle.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (collection == null) {
            EmptyState("Nada que mostrar.")
        } else {
            val live = remember(collection, map) { collection.tracks.filter { it.id in map } }
            TrackList(vm, live)
        }
    }
}
