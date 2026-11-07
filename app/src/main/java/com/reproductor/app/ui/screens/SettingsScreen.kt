package com.reproductor.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.ui.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val lyricsTree by vm.lyricsTree.collectAsStateWithLifecycle()
    val hidden by vm.hiddenCount.collectAsStateWithLifecycle()
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            vm.setLyricsTree(uri.toString())
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        Text("Apariencia", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sistema", "Claro", "Oscuro").forEachIndexed { i, label ->
                FilterChip(selected = theme == i, onClick = { vm.setTheme(i) }, label = { Text(label) })
            }
        }
        HorizontalDivider()

        Text("Letras de canciones", style = MaterialTheme.typography.titleLarge)
        Text(
            "Reproductor busca archivos .lrc (sincronizados) o .txt con el mismo nombre que la canción. " +
                "Desde Android 11 la app no puede leerlos junto a la música, así que elige una carpeta que contenga tus letras (se leen también las subcarpetas).",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (lyricsTree == null) "Carpeta de letras: ninguna"
            else "Carpeta de letras: ${Uri.decode(lyricsTree!!).substringAfterLast(':')}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickFolder.launch(null) }) { Text("Elegir carpeta") }
            if (lyricsTree != null) {
                OutlinedButton(onClick = { vm.reindexLyrics() }) { Text("Reindexar") }
                OutlinedButton(onClick = { vm.setLyricsTree(null) }) { Text("Quitar") }
            }
        }
        HorizontalDivider()

        Text("Biblioteca", style = MaterialTheme.typography.titleLarge)
        Text(
            "${tracks.size} canciones" + if (scanning) " · actualizando…" else "",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.refreshLibrary() }) { Text("Buscar música nueva") }
            if (hidden > 0) {
                OutlinedButton(onClick = { vm.restoreHidden() }) { Text("Restaurar ocultas ($hidden)") }
            }
        }
        Text(
            "Quitar una canción de la biblioteca solo la oculta en esta app: el archivo nunca se borra del dispositivo.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()

        Text("Acerca de", style = MaterialTheme.typography.titleLarge)
        Text(
            "Reproductor 1.0.0\nSin anuncios, sin rastreadores, sin analíticas y sin permiso de Internet: " +
                "todo funciona sin conexión.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
