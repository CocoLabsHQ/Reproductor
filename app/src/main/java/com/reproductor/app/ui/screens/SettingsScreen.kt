package com.reproductor.app.ui.screens

import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
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
    val logCount by vm.activeLogCount.collectAsStateWithLifecycle()
    val trashCount by vm.deletedLogCount.collectAsStateWithLifecycle()
    val savedKey by vm.youtubeKey.collectAsStateWithLifecycle()
    var keyText by remember(savedKey) { mutableStateOf(savedKey.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmPurge by remember { mutableStateOf(false) }
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
        if (logCount > 0) {
            OutlinedButton(onClick = { confirmDelete = true }) { Text("Borrar datos del resumen") }
        }
        if (trashCount > 0) {
            Text(
                "Hay $trashCount registros borrados que aún se pueden recuperar.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.restoreRecapData() }) { Text("Recuperar datos borrados") }
                TextButton(onClick = { confirmPurge = true }) { Text("Eliminar para siempre") }
            }
        }
        HorizontalDivider()

        Text("Videoclips de YouTube", style = MaterialTheme.typography.titleLarge)
        Text(
            "Para ver los videoclips dentro de la app, a pantalla completa y con la proporción de tu pantalla, hace falta una clave gratuita de la API de YouTube Data v3: " +
                "Google Cloud Console → crear proyecto → habilitar «YouTube Data API v3» → Credenciales → Crear clave de API. " +
                "Es tu clave y se guarda solo en este teléfono. Sin clave, los videoclips se buscan en el navegador interno de YouTube.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = keyText, onValueChange = { keyText = it }, singleLine = true,
            label = { Text("Clave de API de YouTube") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.setYoutubeKey(keyText) }) { Text("Guardar") }
            OutlinedButton(enabled = keyText.isNotBlank(), onClick = { vm.testYoutubeKey(keyText) }) { Text("Probar") }
            if (savedKey != null) {
                OutlinedButton(onClick = { vm.setYoutubeKey(null); keyText = "" }) { Text("Quitar") }
            }
        }
        HorizontalDivider()

        Text("Letras de canciones", style = MaterialTheme.typography.titleLarge)
        Text(
            "Music Box busca archivos .lrc (sincronizados) o .txt con el mismo nombre que la canción. " +
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
            Button(onClick = { vm.reportIssue("Error de reproducción", err) }) { Text("Reportar en GitHub") }
        }
        OutlinedButton(onClick = { vm.reportIssue("Problema en Music Box", "(describe el problema)") }) {
            Text("Reportar un problema")
        }
        Text(
            "Se abre GitHub con el texto ya escrito; tú lo revisas y decides si lo envías. La app nunca envía nada por sí sola.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()

        Text("Acerca de", style = MaterialTheme.typography.titleLarge)
        Text(
            "Music Box · Reproductor de música ${vm.currentVersion}\nSin anuncios, sin rastreadores ni analíticas. La música, las playlists y los resúmenes funcionan sin conexión; " +
                "Internet solo se usa para los videoclips (cuando tú los pides) y para buscar actualizaciones.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Borrar los datos del resumen?") },
            text = {
                Text(
                    "Se vaciarán tus resúmenes mensuales y el anual. Los archivos de música no se tocan. " +
                        "Podrás recuperar estos datos desde Ajustes mientras no los elimines para siempre.",
                )
            },
            confirmButton = { TextButton(onClick = { vm.deleteRecapData(); confirmDelete = false }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
    if (confirmPurge) {
        AlertDialog(
            onDismissRequest = { confirmPurge = false },
            title = { Text("¿Eliminar para siempre?") },
            text = { Text("Los datos borrados dejarán de poder recuperarse. Esta acción no se puede deshacer.") },
            confirmButton = { TextButton(onClick = { vm.purgeRecapData(); confirmPurge = false }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmPurge = false }) { Text("Cancelar") } },
        )
    }
}
