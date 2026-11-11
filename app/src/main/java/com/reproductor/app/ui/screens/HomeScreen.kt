package com.reproductor.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.R
import com.reproductor.app.data.TrackCollection
import com.reproductor.app.domain.Recap
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.EmptyState
import com.reproductor.app.ui.components.SectionTitle
import com.reproductor.app.ui.components.TrackArt
import com.reproductor.app.ui.components.TrackRow
import com.reproductor.app.ui.theme.LocalResolvedTheme
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenLibraryTab: (Int) -> Unit,
    onOpenCollection: () -> Unit,
    onOpenStyles: () -> Unit,
    onOpenRecap: () -> Unit,
) {
    val theme = LocalResolvedTheme.current
    val recents by vm.recents.collectAsStateWithLifecycle()
    val added by vm.recentlyAdded.collectAsStateWithLifecycle()
    val all by vm.allTracks.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val recap by vm.currentRecap.collectAsStateWithLifecycle()
    val trackMap by vm.trackMap.collectAsStateWithLifecycle()

    SideEffect { vm.selection.page = added.take(10) }

    // La cabecera se oculta sola a los pocos segundos, o en cuanto el usuario toca o desliza.
    var headerVisible by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        delay(4500)
        headerVisible = false
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { if (it) headerVisible = false }
    }

    Column(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed }) headerVisible = false
                }
            }
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Music Box", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenStyles) { Icon(Icons.Rounded.Palette, "Estilos", tint = MaterialTheme.colorScheme.primary) }
        }

        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(700)) + shrinkVertically(tween(700)),
                ) {
                    Box(
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp)).background(theme.headerBrush()),
                    ) {
                        Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("¡Hola!", style = MaterialTheme.typography.headlineLarge, color = theme.onHeader)
                                Text(
                                    "${all.size} canciones listas para sonar",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = theme.onHeader.copy(alpha = 0.9f),
                                )
                            }
                            Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(84.dp))
                        }
                    }
                }
            }
            item {
                SectionTitle("Accesos rápidos", Modifier.padding(top = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { QuickCard("Aleatorio", Icons.Rounded.Shuffle) { vm.shufflePlay(all) } }
                    item {
                        QuickCard("Añadidas", Icons.Rounded.LibraryMusic) {
                            vm.collection = TrackCollection("Añadidas recientemente", "${added.size} canciones", added)
                            onOpenCollection()
                        }
                    }
                    item { QuickCard("Videos", Icons.Rounded.Videocam) { onOpenLibraryTab(6) } }
                    item { QuickCard("GIF", Icons.Rounded.Image) { onOpenLibraryTab(7) } }
                }
            }
            val r = recap
            if (r != null && r.totals.plays > 0) {
                item { MonthCard(vm, r, onOpen = { vm.openCurrentRecap() }, onAll = onOpenRecap, artFor = { trackMap[it] }) }
            }
            item { SectionTitle("Reproducidas recientemente", Modifier.padding(top = 8.dp)) }
            if (recents.isEmpty()) {
                item {
                    Text(
                        "Aún no has reproducido nada. ¡Elige una canción en la biblioteca!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recents.take(15), key = { it.id }) { t ->
                            Column(Modifier.width(120.dp).clickable { vm.play(recents, recents.indexOf(t)) }) {
                                TrackArt(t, Modifier.size(120.dp), px = 320, shape = RoundedCornerShape(20.dp), iconSize = 40.dp)
                                Text(
                                    t.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp),
                                )
                                Text(
                                    t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Añadidas recientemente", Modifier.padding(top = 12.dp)) }
            if (added.isEmpty()) {
                item { Box(Modifier.height(160.dp)) { EmptyState("No hay música todavía. Concede el permiso y copia canciones al dispositivo.") } }
            } else {
                val top = added.take(10)
                items(top, key = { it.id }) { t ->
                    TrackRow(t, isCurrent = state.currentId == t.id, onClick = { vm.play(top, top.indexOf(t)) })
                }
            }
        }
    }
}

/** Tarjeta "Resumen de <mes>" con las canciones más escuchadas. */
@Composable
private fun MonthCard(
    vm: MainViewModel,
    recap: Recap,
    onOpen: () -> Unit,
    onAll: () -> Unit,
    artFor: (Long) -> com.reproductor.app.data.Track?,
) {
    val theme = LocalResolvedTheme.current
    Column(Modifier.padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onAll).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Resumen de música", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                "Nuevo", style = MaterialTheme.typography.labelLarge, color = theme.onAccent,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(theme.accent).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null)
        }
        Column(
            Modifier.padding(horizontal = 12.dp).fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)).background(theme.headerBrush())
                .clickable(onClick = onOpen).padding(16.dp),
        ) {
            Text(recap.title.uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = theme.onHeader)
            Text(
                "${recap.totals.ms / 60_000} min · ${recap.totals.plays} reproducciones",
                style = MaterialTheme.typography.bodyMedium, color = theme.onHeader.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(10.dp))
            recap.topSongs.take(3).forEachIndexed { i, s ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = MaterialTheme.typography.titleLarge, color = theme.onHeader, modifier = Modifier.width(28.dp))
                    TrackArt(artFor(s.trackId), Modifier.size(40.dp), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = theme.onHeader)
                        Text(s.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = theme.onHeader.copy(alpha = 0.8f))
                    }
                }
            }
            Text("Toca para ver tu resumen", style = MaterialTheme.typography.labelLarge, color = theme.onHeader.copy(alpha = 0.85f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun QuickCard(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
