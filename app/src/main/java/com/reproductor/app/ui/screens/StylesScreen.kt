package com.reproductor.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.theme.LocalResolvedTheme
import com.reproductor.app.ui.theme.ThemeBackground
import com.reproductor.app.ui.theme.Themes

@Composable
fun StylesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val theme = LocalResolvedTheme.current
    val prefs by vm.appearance.collectAsStateWithLifecycle()
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setCustomImage(uri)
    }
    val pickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
            Text("Estilos", style = MaterialTheme.typography.headlineMedium)
        }

        // Vista previa
        Box(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(190.dp)
                .clip(RoundedCornerShape(28.dp)),
        ) {
            ThemeBackground(theme)
            Column(Modifier.padding(16.dp)) {
                Text("Canciones", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(10.dp))
                repeat(2) {
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(theme.accent.copy(alpha = 0.45f)))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Box(Modifier.height(10.dp).width(130.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.height(8.dp).width(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)))
                        }
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(14.dp).size(44.dp).clip(CircleShape).background(theme.accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PlayArrow, null, tint = theme.onAccent) }
        }

        // Temas
        Text("Temas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Themes.presets, key = { it.id }) { spec ->
                val selected = prefs.themeId == spec.id
                Column(Modifier.width(88.dp).clickable { vm.setThemeId(spec.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(88.dp, 120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(spec.top, spec.mid, spec.bottom)))
                            .then(if (selected) Modifier.border(3.dp, theme.accent, RoundedCornerShape(20.dp)) else Modifier),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.padding(bottom = 10.dp).size(18.dp).clip(CircleShape).background(spec.accent))
                    }
                    Text(
                        spec.name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            item {
                val selected = prefs.themeId == "custom"
                Column(
                    Modifier.width(88.dp).clickable { if (prefs.imagePath != null) vm.setThemeId("custom") else pickImage() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(88.dp, 120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(if (selected) Modifier.border(3.dp, theme.accent, RoundedCornerShape(20.dp)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
                    Text(
                        "Mi imagen", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        // Color de acento
        Text("Color", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .border(2.dp, if (prefs.accent == 0) theme.accent else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { vm.setAccent(0) },
                    contentAlignment = Alignment.Center,
                ) { Text("Auto", style = MaterialTheme.typography.labelLarge) }
            }
            items(Themes.accents, key = { it.toArgb() }) { color ->
                val selected = prefs.accent == color.toArgb()
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(color)
                        .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
                        .clickable { vm.setAccent(color.toArgb()) },
                )
            }
        }

        // Ajustes de la imagen personalizada
        if (prefs.themeId == "custom") {
            Text("Imagen de fondo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 20.dp))
            OutlinedButton(onClick = pickImage, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Cambiar imagen")
            }
            Text("Transparencia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
            Slider(
                value = prefs.transparency, onValueChange = { vm.setTransparency(it) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text("Borroso", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
            Slider(
                value = prefs.blur, onValueChange = { vm.setBlur(it) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Modo claro/oscuro (solo para el tema Clásico)
        if (prefs.themeId == "classic") {
            Text("Modo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sistema", "Claro", "Oscuro").forEachIndexed { i, label ->
                    FilterChip(selected = mode == i, onClick = { vm.setTheme(i) }, label = { Text(label) })
                }
            }
        } else {
            Text(
                "Este tema define sus propios colores. Elige «Clásico» para usar el modo claro u oscuro.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
            )
        }
    }
}
