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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.ui.MainViewModel

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, onOpenStyles: () -> Unit, onOpenRecap: () -> Unit) {
    val context = LocalContext.current
    val lyricsTree by vm.lyricsTree.collectAsStateWithLifecycle()
    val hidden by vm.hiddenCount.collectAsStateWithLifecycle()
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val hideWa by vm.hideWhatsApp.collectAsStateWithLifecycle()
    val waCount by vm.whatsAppCount.collectAsStateWithLifecycle()
    val autoUpdate by vm.autoUpdate.collectAsStateWithLifecycle()
    val lastError by vm.lastError.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

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
        Text(
            "Temas, imagen de fondo de tu galería, color y modo claro u oscuro.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onOpenStyles) { Text("Estilos") }
        HorizontalDivider()

        Text("Biblioteca", style = MaterialTheme.typography.titleLarge)
        Text(
            "${tracks.size} canciones" + if (scanning) " · actualizando…" else "",
            style = MaterialTheme.typography.bodyMedium,
        )
        SwitchRow(
            title = "Ocultar audios de WhatsApp",
            description = "Se detectan por la carpeta (WhatsApp Audio, notas de voz…) y por el nombre del archivo (AUD-…-WA0001). " +
                if (waCount > 0) "Ahora hay $waCount en tu dispositivo." else "No se encontró ninguno.",
            checked = hideWa,
            onChange = { vm.setHideWhatsApp(it) },
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

        Text("Resumen de música", style = MaterialTheme.typography.titleLarge)
        Text(
            "La app lleva la cuenta de lo que escuchas (solo en tu teléfono) para mostrarte resúmenes mensuales y, tras un año, tu resumen anual.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onOpenRecap) { Text("Ver mis resúmenes") }
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

        Text("Actualizaciones", style = MaterialTheme.typography.titleLarge)
        SwitchRow(
            title = "Buscar actualizaciones al abrir",
            description = "Consulta las versiones publicadas en GitHub. Es lo único (junto con los videoclips) que usa Internet.",
            checked = autoUpdate,
            onChange = { vm.setAutoUpdate(it) },
        )
        OutlinedButton(onClick = { vm.checkForUpdates(manual = true) }) { Text("Buscar ahora") }
        HorizontalDivider()

        Text("Diagnóstico", style = MaterialTheme.typography.titleLarge)
        val err = lastError
        if (err == null) {
            Text(
                "Sin errores de reproducción. Si una canción no suena, aquí aparecerá el motivo.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SelectionContainer { Text(err, style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { clipboard.setText(AnnotatedString(err)) }) { Text("Copiar") }
                OutlinedButton(onClick = { vm.lastError.value = null }) { Text("Borrar") }
            }
        }
        HorizontalDivider()

        Text("Acerca de", style = MaterialTheme.typography.titleLarge)
        Text(
            "Reproductor ${vm.currentVersion}\nSin anuncios, sin rastreadores ni analíticas. La música, las playlists y los resúmenes funcionan sin conexión; " +
                "Internet solo se usa para los videoclips (cuando tú los pides) y para buscar actualizaciones.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
