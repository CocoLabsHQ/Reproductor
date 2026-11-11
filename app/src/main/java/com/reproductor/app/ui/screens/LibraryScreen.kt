package com.reproductor.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.reproductor.app.data.Track
import com.reproductor.app.data.TrackCollection
import com.reproductor.app.data.formatDuration
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.EmptyState
import com.reproductor.app.ui.components.PlayShuffleButtons
import com.reproductor.app.ui.components.TrackArt
import com.reproductor.app.ui.components.TrackRow
import com.reproductor.app.ui.components.VideoThumb

private val tabs = listOf("Canciones", "Artistas", "Álbumes", "Carpetas", "Géneros", "Recientes", "Videos", "GIF")

@Composable
fun LibraryScreen(vm: MainViewModel, onOpenCollection: () -> Unit) {
    val tab = vm.libraryTab
    Column(Modifier.fillMaxSize()) {
        Text(
            "Biblioteca", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 8.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i, onClick = { vm.libraryTab = i },
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        Crossfade(targetState = tab, label = "library-tab", modifier = Modifier.weight(1f)) { t ->
            when (t) {
                0 -> SongsTab(vm)
                1 -> ArtistsTab(vm, onOpenCollection)
                2 -> AlbumsTab(vm, onOpenCollection)
                3 -> FoldersTab(vm, onOpenCollection)
                4 -> GenresTab(vm, onOpenCollection)
                5 -> RecentTab(vm)
                6 -> VideosTab(vm)
                else -> GifsTab(vm)
            }
        }
    }
}

@Composable
fun TrackList(vm: MainViewModel, tracks: List<Track>, header: Boolean = true) {
    val state by vm.playerState.collectAsStateWithLifecycle()
    SideEffect { vm.selection.page = tracks }
    if (tracks.isEmpty()) {
        EmptyState("No hay canciones aquí todavía.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        if (header) {
            item {
                PlayShuffleButtons(
                    onPlay = { vm.play(tracks, 0) },
                    onShuffle = { vm.shufflePlay(tracks) },
                )
            }
        }
        items(tracks, key = { it.id }) { t ->
            TrackRow(t, isCurrent = state.currentId == t.id, onClick = { vm.play(tracks, tracks.indexOf(t)) })
        }
    }
}

@Composable
private fun SongsTab(vm: MainViewModel) {
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    if (tracks.isEmpty()) {
        EmptyState(if (scanning) "Buscando música en tu dispositivo…" else "No se encontró música. Copia archivos de audio al dispositivo y vuelve aquí.")
    } else {
        TrackList(vm, tracks)
    }
}

@Composable
private fun RecentTab(vm: MainViewModel) {
    val tracks by vm.recentlyAdded.collectAsStateWithLifecycle()
    TrackList(vm, tracks)
}

@Composable
private fun GroupRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ArtistsTab(vm: MainViewModel, onOpen: () -> Unit) {
    val artists by vm.artists.collectAsStateWithLifecycle()
    if (artists.isEmpty()) { EmptyState("Sin artistas todavía."); return }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(artists, key = { it.name }) { a ->
            GroupRow(a.name, "${a.tracks.size} canciones", Icons.Rounded.Person) {
                vm.collection = TrackCollection(a.name, "${a.tracks.size} canciones", a.tracks)
                onOpen()
            }
        }
    }
}

@Composable
private fun AlbumsTab(vm: MainViewModel, onOpen: () -> Unit) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    if (albums.isEmpty()) { EmptyState("Sin álbumes todavía."); return }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(albums, key = { it.id }) { al ->
            Column(Modifier.clickable {
                vm.collection = TrackCollection(al.name, "${al.artist} · ${al.tracks.size} canciones", al.tracks)
                onOpen()
            }) {
                TrackArt(
                    al.tracks.firstOrNull(), Modifier.fillMaxWidth().aspectRatio(1f),
                    px = 400, shape = RoundedCornerShape(22.dp), iconSize = 48.dp,
                )
                Text(al.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                Text(al.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FoldersTab(vm: MainViewModel, onOpen: () -> Unit) {
    val folders by vm.folders.collectAsStateWithLifecycle()
    if (folders.isEmpty()) { EmptyState("Sin carpetas todavía."); return }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(folders, key = { it.path }) { f ->
            GroupRow(f.name, "${f.tracks.size} canciones · ${f.path}", Icons.Rounded.Folder) {
                vm.collection = TrackCollection(f.name, f.path, f.tracks)
                onOpen()
            }
        }
    }
}

@Composable
private fun GenresTab(vm: MainViewModel, onOpen: () -> Unit) {
    val genres by vm.genres.collectAsStateWithLifecycle()
    if (genres.isEmpty()) { EmptyState("Tus archivos no incluyen género en los metadatos (o tu versión de Android no lo expone)."); return }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(genres, key = { it.name }) { g ->
            GroupRow(g.name, "${g.tracks.size} canciones", Icons.Rounded.MusicNote) {
                vm.collection = TrackCollection(g.name, "${g.tracks.size} canciones", g.tracks)
                onOpen()
            }
        }
    }
}

@Composable
private fun VideosTab(vm: MainViewModel) {
    val videos by vm.videos.collectAsStateWithLifecycle()
    if (videos.isEmpty()) { EmptyState("No se encontraron videos (o falta el permiso de videos)."); return }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(videos, key = { it.id }) { v ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.playVideo(v) }.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VideoThumb(v, Modifier.size(width = 96.dp, height = 60.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(formatDuration(v.durationMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GifsTab(vm: MainViewModel) {
    val gifs by vm.gifs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (gifs.isEmpty()) { EmptyState("No se encontraron GIF (o falta el permiso de imágenes)."); return }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(gifs, key = { it.id }) { g ->
            // En la cuadrícula se muestra solo el primer fotograma (no animado) para ahorrar CPU y batería.
            AsyncImage(
                model = ImageRequest.Builder(context).data(g.uri).size(300)
                    .decoderFactory(BitmapFactoryDecoder.Factory()).build(),
                contentDescription = g.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { vm.openGif = g },
            )
        }
    }
}
