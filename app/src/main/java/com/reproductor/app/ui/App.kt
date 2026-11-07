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
import com.reproductor.app.ui.components.LocalTrackActions
import com.reproductor.app.ui.components.MiniPlayer
import com.reproductor.app.ui.components.TrackActions
import com.reproductor.app.ui.screens.CollectionScreen
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
fun App(vm: MainViewModel, isDark: Boolean) {
    val context = LocalContext.current
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
            addToPlaylist = { vm.trackForPlaylistDialog = it },
            hide = { vm.trackToHide = it },
        )
    }
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val selectedTab = when (route) {
        "collection" -> "library"
        "playlist" -> "playlists"
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

    CompositionLocalProvider(LocalTrackActions provides actions) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column {
                        if (state.currentId != null) {
                            MiniPlayer(state, current, vm.player, onOpen = { vm.playerOpen = true })
                        }
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                            vm, isDark,
                            onOpenLibraryTab = { vm.libraryTab = it; goTab("library") },
                            onOpenCollection = { nav.navigate("collection") },
                        )
                    }
                    composable("library") { LibraryScreen(vm, onOpenCollection = { nav.navigate("collection") }) }
                    composable("collection") { CollectionScreen(vm, onBack = { nav.popBackStack() }) }
                    composable("playlists") {
                        PlaylistsScreen(vm, onOpen = { id -> vm.openPlaylistId = id; nav.navigate("playlist") })
                    }
                    composable("playlist") { PlaylistDetailScreen(vm, onBack = { nav.popBackStack() }) }
                    composable("search") { SearchScreen(vm) }
                    composable("settings") { SettingsScreen(vm) }
                }
            }

            // Reproductor a pantalla completa
            AnimatedVisibility(
                visible = vm.playerOpen,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                PlayerScreen(vm, isDark, onClose = { vm.playerOpen = false })
            }

            vm.openVideo?.let { v -> VideoScreen(v, onClose = { vm.openVideo = null }) }
            vm.openGif?.let { g -> GifScreen(g, onClose = { vm.openGif = null }) }
        }

        AddToPlaylistDialog(vm)
        HideTrackDialog(vm)
    }
}

@Composable
private fun AddToPlaylistDialog(vm: MainViewModel) {
    val track = vm.trackForPlaylistDialog ?: return
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf("") }
    val close = { vm.trackForPlaylistDialog = null }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Añadir a una playlist") },
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
                                .clickable { vm.addToPlaylist(p.id, p.name, track); close() }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotBlank(),
                onClick = { vm.createPlaylist(newName, track); close() },
            ) { Text("Crear y añadir") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } },
    )
}

@Composable
private fun HideTrackDialog(vm: MainViewModel) {
    val track = vm.trackToHide ?: return
    AlertDialog(
        onDismissRequest = { vm.trackToHide = null },
        title = { Text("¿Quitar de la biblioteca?") },
        text = {
            Text(
                "«${track.title}» dejará de mostrarse en Reproductor. El archivo NO se borra del dispositivo " +
                    "y puedes restaurarla desde Ajustes.",
            )
        },
        confirmButton = { TextButton(onClick = { vm.hide(track); vm.trackToHide = null }) { Text("Quitar") } },
        dismissButton = { TextButton(onClick = { vm.trackToHide = null }) { Text("Cancelar") } },
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
