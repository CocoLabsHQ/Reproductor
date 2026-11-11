package com.reproductor.app.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.reproductor.app.data.AppearancePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.roundToInt

enum class Decor { NONE, STARS, BOKEH, WAVES, HILLS }

/** Un tema prediseñado: degradado de tres colores + decoración dibujada por código (sin imágenes pesadas). */
data class ThemeSpec(
    val id: String,
    val name: String,
    val dark: Boolean,
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val accent: Color,
    val decor: Decor,
)

object Themes {
    val classic = ThemeSpec("classic", "Clásico", false, Cyan, Teal, Color(0xFF0DB39F), Rooster, Decor.NONE)

    val presets: List<ThemeSpec> = listOf(
        classic,
        ThemeSpec("sunset", "Atardecer", true, Color(0xFF1B1035), Color(0xFF7B2A6B), Color(0xFFD2562E), Color(0xFFFFB74D), Decor.HILLS),
        ThemeSpec("forest", "Bosque", true, Color(0xFF0B2B20), Color(0xFF14543B), Color(0xFF1F7A4D), Color(0xFFB9F6CA), Decor.BOKEH),
        ThemeSpec("ocean", "Océano", true, Color(0xFF03253D), Color(0xFF0A5C8A), Color(0xFF16A6B6), Color(0xFF7FE9FF), Decor.WAVES),
        ThemeSpec("neon", "Neón", true, Color(0xFF0B0620), Color(0xFF2A0B55), Color(0xFF5B1AA6), Color(0xFFFF4FD8), Decor.STARS),
        ThemeSpec("midnight", "Medianoche", true, Color(0xFF050B1A), Color(0xFF0F2A44), Color(0xFF1B4965), Color(0xFFF5B82E), Decor.STARS),
        ThemeSpec("aurora", "Aurora", true, Color(0xFF04141F), Color(0xFF0B5D5A), Color(0xFF4B2A7B), Color(0xFF69F0AE), Decor.STARS),
        ThemeSpec("candy", "Caramelo", false, Color(0xFFFFE3EC), Color(0xFFFFC9DE), Color(0xFFFFDDB8), Color(0xFFE91E63), Decor.BOKEH),
        ThemeSpec("mint", "Menta", false, Color(0xFFE6FFF7), Color(0xFFBDF4E3), Color(0xFF9BE3F0), Color(0xFF00897B), Decor.WAVES),
    )

    val custom = ThemeSpec("custom", "Mi imagen", true, Color(0xFF14142B), Color(0xFF1B1B3A), Color(0xFF0E0E20), Color(0xFFFFA000), Decor.NONE)

    fun byId(id: String): ThemeSpec =
        presets.firstOrNull { it.id == id } ?: if (id == "custom") custom else classic

    val accents: List<Color> = listOf(
        Color(0xFFFFA000), Color(0xFFF57C00), Color(0xFFF44336), Color(0xFFE91E63), Color(0xFFC2185B),
        Color(0xFFC0CA33), Color(0xFF66BB6A), Color(0xFF00BFA5), Color(0xFF26C6DA), Color(0xFF42A5F5),
        Color(0xFF7E57C2), Color(0xFFAB47BC), Color(0xFFFFD54F),
    )
}

/** Tema ya resuelto (modo claro/oscuro, acento y preferencias aplicados). */
@Immutable
data class ResolvedTheme(
    val spec: ThemeSpec,
    val prefs: AppearancePrefs,
    val dark: Boolean,
    val accent: Color,
) {
    val isClassic: Boolean get() = spec.id == "classic"
    val isCustom: Boolean get() = spec.id == "custom"
    val onAccent: Color get() = if (accent.luminance() > 0.55f) Color(0xFF10202A) else Color.White

    private val usesBrandHeader: Boolean get() = isClassic && prefs.accent == 0

    /** Color del texto sobre la cabecera. */
    val onHeader: Color get() = if (usesBrandHeader) Color.White else onAccent

    /** Cabecera de Inicio: combina con el acento del tema. */
    fun headerBrush(): Brush =
        if (usesBrandHeader) brandBrush(dark)
        else Brush.horizontalGradient(listOf(accent, lerp(accent, spec.mid, 0.55f)))
}

val LocalResolvedTheme = staticCompositionLocalOf<ResolvedTheme> { error("ResolvedTheme no proporcionado") }

fun resolveTheme(prefs: AppearancePrefs, mode: Int, systemDark: Boolean): ResolvedTheme {
    val spec = Themes.byId(prefs.themeId)
    val dark = if (spec.id == "classic") {
        when (mode) { 1 -> false; 2 -> true; else -> systemDark }
    } else spec.dark
    val accent = when {
        prefs.accent != 0 -> Color(prefs.accent)
        spec.id == "classic" -> if (dark) Color(0xFFFF5A52) else Rooster
        else -> spec.accent
    }
    return ResolvedTheme(spec, prefs, dark, accent)
}

/** Paleta Material derivada del tema elegido. */
fun ResolvedTheme.colorScheme(): ColorScheme {
    val onAcc = onAccent
    if (isClassic) {
        val base = if (dark) DarkColors else LightColors
        return base.copy(
            primary = accent, onPrimary = onAcc,
            primaryContainer = lerp(base.surface, accent, 0.25f), onPrimaryContainer = base.onSurface,
        )
    }
    val surface = if (dark) lerp(spec.mid, Color.Black, 0.55f) else lerp(spec.mid, Color.White, 0.55f)
    val onS = if (dark) Color(0xFFF2F7F9) else Color(0xFF0F2A44)
    val onSv = onS.copy(alpha = 0.72f)
    val pc = lerp(surface, accent, 0.30f)
    val sc = lerp(surface, accent, 0.22f)
    val sv = lerp(surface, accent, 0.14f)
    return if (dark) {
        darkColorScheme(
            primary = accent, onPrimary = onAcc, primaryContainer = pc, onPrimaryContainer = onS,
            secondary = accent, onSecondary = onAcc, secondaryContainer = sc, onSecondaryContainer = onS,
            tertiary = accent, onTertiary = onAcc,
            background = spec.top, onBackground = onS, surface = surface, onSurface = onS,
            surfaceVariant = sv, onSurfaceVariant = onSv, outline = onSv,
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = onAcc, primaryContainer = pc, onPrimaryContainer = onS,
            secondary = accent, onSecondary = onAcc, secondaryContainer = sc, onSecondaryContainer = onS,
            tertiary = accent, onTertiary = onAcc,
            background = spec.top, onBackground = onS, surface = surface, onSurface = onS,
            surfaceVariant = sv, onSurfaceVariant = onSv, outline = onSv,
        )
    }
}

/** Fondo de toda la app (o del reproductor a pantalla completa si [player] es true). */
@Composable
fun ThemeBackground(theme: ResolvedTheme, modifier: Modifier = Modifier, player: Boolean = false) {
    val spec = theme.spec
    Box(modifier.fillMaxSize()) {
        when {
            theme.isClassic && !player ->
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            theme.isClassic ->
                Box(Modifier.fillMaxSize().background(brandBrush(theme.dark)))
            theme.isCustom -> CustomImageBackground(theme, player)
            else -> {
                val brush = if (player && !spec.dark) {
                    Brush.verticalGradient(listOf(lerp(theme.accent, Color.Black, 0.35f), lerp(theme.accent, Color.Black, 0.65f)))
                } else {
                    Brush.verticalGradient(listOf(spec.top, spec.mid, spec.bottom))
                }
                Box(Modifier.fillMaxSize().background(brush))
                DecorLayer(spec.decor, theme.accent, lightBg = !spec.dark && !player)
            }
        }
    }
}

@Composable
private fun CustomImageBackground(theme: ResolvedTheme, player: Boolean) {
    val prefs = theme.prefs
    val bucket = (prefs.blur * 10).roundToInt()
    val bmp by produceState<Bitmap?>(null, prefs.imagePath, prefs.imageVersion, bucket) {
        value = withContext(Dispatchers.Default) { runCatching { loadBackground(prefs.imagePath, prefs.blur) }.getOrNull() }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF14142B))) {
        val b = bmp
        if (b != null) {
            val blurMod = if (Build.VERSION.SDK_INT >= 31 && prefs.blur > 0f) Modifier.blur((prefs.blur * 16f).dp) else Modifier
            Image(
                bitmap = b.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize().then(blurMod),
            )
        }
        // "Transparencia": cuanto más alta, menos se oscurece la imagen.
        val scrim = (0.75f * (1f - prefs.transparency) + if (player) 0.15f else 0f).coerceIn(0f, 0.9f)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))
    }
}

private fun loadBackground(path: String?, blur: Float): Bitmap? {
    if (path == null) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 1080) sample *= 2
    val src = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    if (blur <= 0.05f) return src
    // Reducir y volver a ampliar con filtro suaviza la imagen en cualquier versión de Android.
    val scale = 1f - blur * 0.93f
    val w = (src.width * scale).toInt().coerceAtLeast(12)
    val h = (src.height * scale).toInt().coerceAtLeast(12)
    return Bitmap.createScaledBitmap(src, w, h, true)
}

@Composable
private fun DecorLayer(decor: Decor, accent: Color, lightBg: Boolean) {
    if (decor == Decor.NONE) return
    Canvas(Modifier.fillMaxSize()) {
        val rnd = Random(7)
        val w = size.width
        val h = size.height
        when (decor) {
            Decor.STARS -> repeat(80) {
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val r = 1f + rnd.nextFloat() * 2.4f
                drawCircle(Color.White.copy(alpha = 0.2f + rnd.nextFloat() * 0.5f), r, Offset(x, y))
            }
            Decor.BOKEH -> repeat(18) {
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val r = 16f + rnd.nextFloat() * 70f
                val c = if (lightBg) accent else accent
                drawCircle(c.copy(alpha = 0.08f + rnd.nextFloat() * 0.12f), r, Offset(x, y))
            }
            Decor.WAVES -> {
                for (i in 0 until 3) {
                    val base = h * (0.72f + i * 0.08f)
                    val p = Path().apply {
                        moveTo(0f, base)
                        cubicTo(w * 0.25f, base - h * 0.06f, w * 0.55f, base + h * 0.07f, w, base - h * 0.03f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(p, (if (lightBg) accent else Color.White).copy(alpha = 0.07f + i * 0.03f))
                }
            }
            Decor.HILLS -> {
                for (i in 0 until 2) {
                    val base = h * (0.80f + i * 0.08f)
                    val p = Path().apply {
                        moveTo(0f, base)
                        cubicTo(w * 0.3f, base - h * 0.12f, w * 0.6f, base + h * 0.02f, w, base - h * 0.08f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(p, Color.Black.copy(alpha = 0.12f + i * 0.08f))
                }
            }
            Decor.NONE -> Unit
        }
    }
}
