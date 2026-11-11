package com.reproductor.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reproductor.app.R
import com.reproductor.app.ui.components.LocalSelection
import com.reproductor.app.ui.components.LocalTrackActions
import com.reproductor.app.ui.components.SelectionBar
import androidx.activity.compose.BackHandler
import com.reproductor.app.ui.components.MiniPlayer
import com.reproductor.app.ui.components.TrackActions
import com.reproductor.app.ui.screens.ClipScreen
import com.reproductor.app.ui.screens.CollectionScreen
import com.reproductor.app.ui.screens.RecapListScreen
import com.reproductor.app.ui.screens.StylesScreen
import com.reproductor.app.ui.screens.WrappedOverlay
import com.reproductor.app.data.UpdateUi
import com.reproductor.app.data.formatBytes
import com.reproductor.app.ui.theme.LocalResolvedTheme
import com.reproductor.app.ui.theme.ThemeBackground
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import com.reproductor.app.ui.screens.GifScreen
import com.reproductor.app.ui.screens.HomeScreen
import com.reproductor.app.ui.screens.LibraryScreen
import com.reproductor.app.ui.screens.PlayerScreen
import com.reproductor.app.ui.screens.PlaylistDetailScreen
import com.reproductor.app.ui.screens.PlaylistsScreen
import com.reproductor.app.ui.screens.SearchScreen
import com.reproductor.app.ui.screens.SettingsScreen
import com.reproductor.app.ui.screens.VideoScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Inicio", Icons.Rounded.Home),
    Tab("library", "Biblioteca", Icons.Rounded.LibraryMusic),
    Tab("playlists", "Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Tab("search", "Buscar", Icons.Rounded.Search),
    Tab("settings", "Ajustes", Icons.Rounded.Settings),
)

@Composable
fun App(vm: MainViewModel) {
    val context = LocalContext.current
    val theme = LocalResolvedTheme.current
    var granted by remember { mutableStateOf(hasAudioPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAudioPermission(context)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasAudioPermission(context) }
    LaunchedEffect(granted) { if (granted) vm.onPermissionGranted() }

    if (!granted) {
        PermissionScreen(
            onRequest = { permissionLauncher.launch(requiredPermissions()) },
            onSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
        return
    }

    val actions = remember(vm) {
        TrackActions(
            playNext = { vm.playNext(it) },
            enqueue = { vm.enqueue(it) },
            addToPlaylist = { vm.tracksForPlaylistDialog = listOf(it) },
            hide = { vm.tracksToHide = listOf(it) },
        )
    }
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val selectedTab = when (route) {
        "collection" -> "library"
        "playlist" -> "playlists"
        "styles" -> "settings"
        "recap" -> "home"
        else -> route
    }
    val state by vm.playerState.collectAsStateWithLifecycle()
    val trackMap by vm.trackMap.collectAsStateWithLifecycle()
    val current = state.currentId?.let { trackMap[it] }

    fun goTab(r: String) {
        if (!nav.popBackStack(r, false)) {
            nav.navigate(r) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val sel = vm.selection
    // La selección se cancela al cambiar de pantalla o de pestaña de la biblioteca.
    LaunchedEffect(route, vm.libraryTab) { sel.clear() }

    CompositionLocalProvider(LocalTrackActions provides actions, LocalSelection provides sel) {
        Box(Modifier.fillMaxSize()) {
            ThemeBackground(theme)
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    if (sel.active) {
                        SelectionBar(
                            sel = sel,
                            onPlay = { val l = sel.tracks; sel.clear(); vm.play(l, 0) },
                            onQueue = { vm.enqueueAll(sel.tracks) },
                            onPlaylist = { vm.tracksForPlaylistDialog = sel.tracks },
                            onHide = { vm.tracksToHide = sel.tracks },
                        )
                    } else Column {
                        if (state.currentId != null) {
                            MiniPlayer(state, current, vm.player, onOpen = { vm.playerOpen = true })
                        }
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                            tabs.forEach { t ->
                                NavigationBarItem(
                                    selected = selectedTab == t.route,
                                    onClick = { goTab(t.route) },
                                    icon = { Icon(t.icon, t.label) },
                                    label = { Text(t.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { inner ->
                NavHost(nav, startDestination = "home", modifier = Modifier.padding(inner)) {
                    composable("home") {
                        HomeScreen(
                            vm,
                            onOpenLibraryTab = { vm.libraryTab = it; goTab("library") },
                            onOpenCollection = { nav.navigate("collection") },
                            onOpenStyles = { nav.navigate("styles") },
                            onOpenRecap = { nav.navigate("recap") },
                        )
                    }
                    composable("library") { LibraryScreen(vm, onOpenCollection = { nav.navigate("collection") }) }
                    composable("collection") { CollectionScreen(vm, onBack = { nav.popBackStack() }) }
                    composable("playlists") {
                        PlaylistsScreen(vm, onOpen = { id -> vm.openPlaylistId = id; nav.navigate("playlist") })
                    }
                    composable("playlist") { PlaylistDetailScreen(vm, onBack = { nav.popBackStack() }) }
                    composable("search") { SearchScreen(vm) }
                    composable("settings") {
                        SettingsScreen(
                            vm,
                            onOpenStyles = { nav.navigate("styles") },
                            onOpenRecap = { nav.navigate("recap") },
                        )
                    }
                    composable("styles") { StylesScreen(vm, onBack = { nav.popBackStack() }) }
                    composable("recap") { RecapListScreen(vm, onBack = { nav.popBackStack() }) }
                }
            }

            BackHandler(enabled = sel.active) { sel.clear() }

            // Reproductor a pantalla completa
            AnimatedVisibility(
                visible = vm.playerOpen,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                PlayerScreen(vm, theme.dark, onClose = { vm.playerOpen = false })
            }

            vm.openVideo?.let { v -> VideoScreen(v, onClose = { vm.openVideo = null }) }
            vm.openGif?.let { g -> GifScreen(g, onClose = { vm.openGif = null }) }
            vm.wrapped?.let { r ->
                WrappedOverlay(
                    recap = r,
                    trackFor = { trackMap[it] },
                    onClip = { artist, title -> vm.openClip(artist, title) },
                    onClose = { vm.wrapped = null },
                )
            }
            vm.clipQuery?.let { q -> ClipScreen(q, onClose = { vm.clipQuery = null }) }
        }

        AddToPlaylistDialog(vm)
        HideTrackDialog(vm)
        UpdateDialog(vm)
        WhatsNewDialog(vm)
    }
}

@Composable
private fun AddToPlaylistDialog(vm: MainViewModel) {
    val tracks = vm.tracksForPlaylistDialog ?: return
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf("") }
    val close = { vm.tracksForPlaylistDialog = null }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (tracks.size == 1) "Añadir a una playlist" else "Añadir ${tracks.size} canciones a una playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it }, singleLine = true,
                    label = { Text("Nueva playlist") }, modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.padding(top = 8.dp)) {
                    items(playlists, key = { it.id }) { p ->
                        Text(
                            "${p.name} (${p.tracks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.addToPlaylist(p.id, p.name, tracks); close() }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotBlank(),
                onClick = { vm.createPlaylist(newName, tracks); close() },
            ) { Text("Crear y añadir") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } },
    )
}

@Composable
private fun HideTrackDialog(vm: MainViewModel) {
    val tracks = vm.tracksToHide ?: return
    AlertDialog(
        onDismissRequest = { vm.tracksToHide = null },
        title = { Text("¿Quitar de la biblioteca?") },
        text = {
            Text(
                if (tracks.size == 1) {
                    "«${tracks[0].title}» dejará de mostrarse en Reproductor. El archivo NO se borra del dispositivo " +
                        "y puedes restaurarla desde Ajustes."
                } else {
                    "${tracks.size} canciones dejarán de mostrarse en Reproductor. Los archivos NO se borran del dispositivo " +
                        "y puedes restaurarlas desde Ajustes."
                },
            )
        },
        confirmButton = { TextButton(onClick = { vm.hide(tracks); vm.tracksToHide = null }) { Text("Quitar") } },
        dismissButton = { TextButton(onClick = { vm.tracksToHide = null }) { Text("Cancelar") } },
    )
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(180.dp))
        Text("Reproductor", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Para encontrar tu música, videos y GIF necesito permiso para leer los archivos multimedia del dispositivo. " +
                "Todo se queda en tu teléfono: la app no usa Internet ni muestra anuncios.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Conceder permiso") }
        TextButton(onClick = onSettings) { Text("Abrir ajustes de la app") }
    }
}

@Composable
private fun UpdateDialog(vm: MainViewModel) {
    when (val u = vm.updateUi) {
        UpdateUi.Hidden -> Unit
        is UpdateUi.Available -> AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text("Hay una actualización") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("La versión ${u.info.version} ya está disponible (tienes la ${vm.currentVersion}).")
                    Text(
                        "Tamaño de la descarga: ${formatBytes(u.info.sizeBytes)}",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp),
                    )
                    if (u.info.notes.isNotBlank()) {
                        Text("Novedades", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                        Text(u.info.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.startUpdate(u.info) }) { Text("Actualizar") } },
            dismissButton = { TextButton(onClick = { vm.dismissUpdate() }) { Text("Más tarde") } },
        )
        is UpdateUi.Downloading -> AlertDialog(
            onDismissRequest = { },
            title = { Text("Descargando actualización…") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { u.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${u.percent} %", modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { },
        )
        is UpdateUi.NeedsPermission -> AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text("Un permiso más") },
            text = {
                Text(
                    "Android necesita que permitas a Reproductor instalar actualizaciones. " +
                        "Activa «Permitir desde esta fuente», vuelve aquí y pulsa «Instalar».",
                )
            },
            confirmButton = { TextButton(onClick = { vm.installUpdate(u.info) }) { Text("Instalar") } },
            dismissButton = { TextButton(onClick = { vm.openInstallPermission() }) { Text("Abrir ajustes") } },
        )
        is UpdateUi.Error -> AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text("No se pudo actualizar") },
            text = { Text(u.message) },
            confirmButton = { TextButton(onClick = { vm.dismissUpdate() }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun WhatsNewDialog(vm: MainViewModel) {
    val notes = vm.whatsNew ?: return
    AlertDialog(
        onDismissRequest = { vm.dismissWhatsNew() },
        title = { Text("Novedades de la versión ${vm.currentVersion}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(notes) } },
        confirmButton = { TextButton(onClick = { vm.dismissWhatsNew() }) { Text("¡Genial!") } },
    )
}
