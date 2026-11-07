package com.reproductor.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.R
import com.reproductor.app.data.TrackCollection
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.EmptyState
import com.reproductor.app.ui.components.SectionTitle
import com.reproductor.app.ui.components.TrackArt
import com.reproductor.app.ui.components.TrackRow
import com.reproductor.app.ui.theme.brandBrush

@Composable
fun HomeScreen(
    vm: MainViewModel,
    isDark: Boolean,
    onOpenLibraryTab: (Int) -> Unit,
    onOpenCollection: () -> Unit,
) {
    val recents by vm.recents.collectAsStateWithLifecycle()
    val added by vm.recentlyAdded.collectAsStateWithLifecycle()
    val all by vm.allTracks.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            // Cabecera con el degradado y el gallo del logotipo
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(brandBrush(isDark)),
            ) {
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("¡Hola!", style = MaterialTheme.typography.headlineLarge, color = androidx.compose.ui.graphics.Color.White)
                        Text(
                            "${all.size} canciones listas para sonar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                        )
                    }
                    Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(84.dp))
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
