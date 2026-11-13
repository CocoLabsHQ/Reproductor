package com.reproductor.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.R
import com.reproductor.app.data.ArtistStat
import com.reproductor.app.data.SongStat
import com.reproductor.app.data.Track
import com.reproductor.app.domain.MonthTop
import com.reproductor.app.domain.Recap
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.EmptyState
import com.reproductor.app.ui.components.TrackArt
import com.reproductor.app.ui.components.blockTouches
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- Lista de resúmenes

@Composable
fun RecapListScreen(vm: MainViewModel, onBack: () -> Unit) {
    val rows by vm.monthRows.collectAsStateWithLifecycle()
    val year by vm.yearStatus.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
            Text("Resúmenes", style = MaterialTheme.typography.headlineMedium)
        }
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(16.dp),
        ) {
            Text("Tu año en música", style = MaterialTheme.typography.titleLarge)
            if (year.unlocked) {
                Text("Ya puedes ver tu resumen anual, con diapositivas.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
                Button(onClick = { vm.openYearRecap() }) { Text("Ver mi resumen del año") }
            } else {
                Text(
                    if (year.hasData) "Se desbloquea tras un año de registro. Faltan ${year.daysLeft} días."
                    else "Se desbloquea tras un año de registro. Empieza a escuchar música para llevar la cuenta.",
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Text("Por mes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
        if (rows.isEmpty()) {
            EmptyState("Todavía no hay datos. Escucha música (más de 20 segundos por canción) y aquí aparecerán tus resúmenes.")
        } else {
            LazyColumn {
                items(rows, key = { it.label }) { r ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.openMonthRecap(r.ym) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(r.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${r.minutes} min", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Diapositivas (estilo "Wrapped")

private sealed interface Slide {
    data class Intro(val title: String, val isYear: Boolean) : Slide
    data class Minutes(val minutes: Long, val plays: Int, val songs: Int) : Slide
    data class TopSong(val song: SongStat) : Slide
    data class TopSongs(val songs: List<SongStat>) : Slide
    data class TopArtists(val artists: List<ArtistStat>) : Slide
    data class Peak(val hour: Int) : Slide
    data class Months(val months: List<MonthTop>) : Slide
    data class Outro(val title: String) : Slide
}

private fun buildSlides(r: Recap): List<Slide> = buildList {
    add(Slide.Intro(r.title, r.isYear))
    add(Slide.Minutes(r.totals.ms / 60_000, r.totals.plays, r.totals.songs))
    r.topSongs.firstOrNull()?.let { add(Slide.TopSong(it)) }
    if (r.topSongs.size > 1) add(Slide.TopSongs(r.topSongs))
    if (r.topArtists.isNotEmpty()) add(Slide.TopArtists(r.topArtists))
    r.peakHour?.let { add(Slide.Peak(it)) }
    if (r.isYear && r.monthly.any { it.song != null }) add(Slide.Months(r.monthly))
    add(Slide.Outro(r.title))
}

private val palettes = listOf(
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF0F9B8E), Color(0xFF1E9BD7)),
    listOf(Color(0xFFF7971E), Color(0xFFD84315)),
    listOf(Color(0xFF0B8A5F), Color(0xFF1E9BD7)),
)

@Composable
fun WrappedOverlay(
    recap: Recap,
    trackFor: (Long) -> Track?,
    onClip: (artist: String, title: String) -> Unit,
    onClose: () -> Unit,
) {
    val slides = remember(recap) { buildSlides(recap) }
    var index by remember(recap) { mutableIntStateOf(0) }
    val progress = remember(index) { Animatable(0f) }
    LaunchedEffect(index) {
        if (index < slides.lastIndex) {
            progress.animateTo(1f, tween(7000, easing = LinearEasing))
            index++
        } else {
            progress.snapTo(1f)
        }
    }
    BackHandler(onBack = onClose)

    Box(
        Modifier.fillMaxSize().background(Color.Black).blockTouches()
            .pointerInput(slides.size) {
                detectTapGestures { offset ->
                    if (offset.x < size.width / 3f) { if (index > 0) index-- }
                    else if (index < slides.lastIndex) index++
                }
            },
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "wrapped-slide",
        ) { i ->
            val colors = palettes[i % palettes.size]
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors))) {
                SlideContent(slides[i], trackFor, onClip, onClose)
            }
        }
        // Barras de progreso estilo "stories"
        Row(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            slides.indices.forEach { i ->
                val p = when {
                    i < index -> 1f
                    i == index -> progress.value
                    else -> 0f
                }
                LinearProgressIndicator(
                    progress = { p }, color = Color.White, trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.weight(1f).height(3.dp).clip(CircleShape),
                )
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(top = 18.dp).align(Alignment.TopEnd)) {
            Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White)
        }
    }
}

@Composable
private fun Reveal(delayMs: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); shown = true }
    AnimatedVisibility(shown, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }) { content() }
}

@Composable
private fun SlideContent(
    slide: Slide,
    trackFor: (Long) -> Track?,
    onClip: (String, String) -> Unit,
    onClose: () -> Unit,
) {
    val white = Color.White
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (slide) {
            is Slide.Intro -> {
                Reveal(0) { Image(painterResource(R.drawable.logo), null, Modifier.size(130.dp)) }
                Reveal(250) {
                    Text(
                        if (slide.isYear) "TU AÑO EN MÚSICA" else "TU RESUMEN DE",
                        style = MaterialTheme.typography.titleMedium, color = white.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
                Reveal(450) {
                    Text(
                        if (slide.isYear) "Music Box" else slide.title.substringBefore(' '),
                        fontSize = 52.sp, fontWeight = FontWeight.Black, color = white, textAlign = TextAlign.Center,
                    )
                }
                Reveal(650) {
                    Text(
                        if (slide.isYear) "Wrapped" else slide.title.substringAfter(' ', ""),
                        fontSize = 30.sp, fontWeight = FontWeight.Bold, color = white.copy(alpha = 0.9f),
                    )
                }
            }
            is Slide.Minutes -> {
                Reveal(0) { Text("Escuchaste", style = MaterialTheme.typography.titleLarge, color = white.copy(alpha = 0.9f)) }
                val shownMinutes by animateCount(slide.minutes)
                Reveal(250) { Text("$shownMinutes", fontSize = 84.sp, fontWeight = FontWeight.Black, color = white) }
                Reveal(500) { Text("minutos de música", style = MaterialTheme.typography.headlineMedium, color = white, textAlign = TextAlign.Center) }
                Reveal(750) {
                    Text(
                        "${slide.plays} reproducciones · ${slide.songs} canciones distintas",
                        style = MaterialTheme.typography.bodyLarge, color = white.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            is Slide.TopSong -> {
                Reveal(0) { Text("Tu canción número 1", style = MaterialTheme.typography.titleLarge, color = white.copy(alpha = 0.9f)) }
                Reveal(250) {
                    TrackArt(trackFor(slide.song.trackId), Modifier.padding(vertical = 20.dp).size(220.dp), px = 600, shape = RoundedCornerShape(28.dp), iconSize = 80.dp)
                }
                Reveal(500) {
                    Text(slide.song.title, style = MaterialTheme.typography.headlineMedium, color = white, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Reveal(650) {
                    Text(
                        "${slide.song.artist} · ${slide.song.plays} ${if (slide.song.plays == 1) "vez" else "veces"}",
                        style = MaterialTheme.typography.bodyLarge, color = white.copy(alpha = 0.9f), textAlign = TextAlign.Center,
                    )
                }
                Reveal(850) { ClipButton { onClip(slide.song.artist, slide.song.title) } }
            }
            is Slide.TopSongs -> {
                Reveal(0) { Text("Tus canciones favoritas", style = MaterialTheme.typography.headlineMedium, color = white, modifier = Modifier.padding(bottom = 16.dp)) }
                slide.songs.forEachIndexed { i, s ->
                    Reveal(200 + i * 150) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClip(s.artist, s.title) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${i + 1}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = white, modifier = Modifier.width(38.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = white)
                                Text("${s.artist} · ${s.plays} reproducciones", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.8f))
                            }
                            Icon(Icons.Rounded.PlayArrow, "Ver videoclip", tint = white)
                        }
                    }
                }
                Reveal(1100) { Text("Toca una canción para ver su videoclip (usa Internet)", style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.75f), modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center) }
            }
            is Slide.TopArtists -> {
                Reveal(0) { Text("Tus artistas más escuchados", style = MaterialTheme.typography.headlineMedium, color = white, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp)) }
                slide.artists.forEachIndexed { i, a ->
                    Reveal(200 + i * 150) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = white, modifier = Modifier.width(38.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge, color = white)
                                Text("${a.ms / 60_000} min", style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
            is Slide.Peak -> {
                Reveal(0) { Text("Tu hora favorita para escuchar", style = MaterialTheme.typography.titleLarge, color = white.copy(alpha = 0.9f), textAlign = TextAlign.Center) }
                Reveal(300) { Text("%02d:00".format(slide.hour), fontSize = 84.sp, fontWeight = FontWeight.Black, color = white) }
                Reveal(600) {
                    Text(
                        when (slide.hour) {
                            in 5..11 -> "Eres de música por la mañana ☀️"
                            in 12..18 -> "La tarde es tu momento 🎧"
                            in 19..23 -> "Tu música suena de noche 🌙"
                            else -> "Eres de madrugadas con música ✨"
                        },
                        style = MaterialTheme.typography.headlineMedium, color = white, textAlign = TextAlign.Center,
                    )
                }
            }
            is Slide.Months -> {
                Reveal(0) { Text("Tu año, mes a mes", style = MaterialTheme.typography.headlineMedium, color = white, modifier = Modifier.padding(bottom = 12.dp)) }
                slide.months.filter { it.song != null }.forEachIndexed { i, m ->
                    Reveal(100 + i * 80) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(m.label.take(3), style = MaterialTheme.typography.titleMedium, color = white.copy(alpha = 0.85f), modifier = Modifier.width(48.dp))
                            Text(
                                "${m.song?.title.orEmpty()} · ${m.song?.artist.orEmpty()}",
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge, color = white, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            is Slide.Outro -> {
                Reveal(0) { Image(painterResource(R.drawable.logo), null, Modifier.size(140.dp)) }
                Reveal(300) { Text("¡Gracias por escuchar!", fontSize = 34.sp, fontWeight = FontWeight.Black, color = white, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp)) }
                Reveal(600) {
                    Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = Color.Black), modifier = Modifier.padding(top = 24.dp)) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        modifier = Modifier.padding(top = 20.dp),
    ) {
        Icon(Icons.Rounded.PlayArrow, null)
        Spacer(Modifier.width(6.dp))
        Text("Ver videoclip")
    }
}

/** Cuenta animada de 0 al valor final. */
@Composable
private fun animateCount(target: Long): State<Long> {
    val anim = remember(target) { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(1400)) }
    return remember(anim) { derivedStateOf { anim.value.toLong() } }
}

// ---------------------------------------------------------------- Videoclip (WebView; usa Internet)

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ClipSearchScreen(query: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    val online = remember(attempt) { isOnline(context) }
    val url = "https://m.youtube.com/results?search_query=" + Uri.encode(query)
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val w = webView
        if (w != null && w.canGoBack()) w.goBack() else onClose()
    }

    Column(Modifier.fillMaxSize().background(Color.Black).blockTouches().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White) }
            Text(query.removeSuffix(" official video"), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) {
                Text("Abrir en YouTube", color = Color.White)
            }
        }
        if (!online) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Sin conexión a Internet. Los videoclips necesitan conexión; el resto de la app funciona sin ella.", color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { attempt++ }, modifier = Modifier.padding(top = 16.dp)) { Text("Reintentar") }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // Solo se permite navegar dentro de YouTube / Google.
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val host = request.url.host.orEmpty()
                                val ok = request.url.scheme == "https" &&
                                    (host.endsWith("youtube.com") || host.endsWith("google.com") || host.endsWith("youtu.be"))
                                return !ok
                            }
                        }
                        loadUrl(url)
                        webView = this
                    }
                },
                onRelease = { it.destroy() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}
