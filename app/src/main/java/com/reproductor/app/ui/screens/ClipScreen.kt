package com.reproductor.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.reproductor.app.data.ClipResult
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.blockTouches
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private const val BASE_URL = "https://com.reproductor.app/"

/**
 * Videoclip incrustado (reproductor oficial de YouTube) a pantalla completa, ajustado a la
 * proporción real de la pantalla (19:9, 19.5:9, 20:9…). Necesita una clave de la API de YouTube en Ajustes;
 * sin clave se ofrece buscar en el navegador interno.
 */
@Composable
fun ClipScreen(vm: MainViewModel, query: String, onClose: () -> Unit, onOpenSettings: () -> Unit) {
    var attempt by remember(query) { mutableIntStateOf(0) }
    var useSearchPage by remember(query) { mutableStateOf(false) }
    val result by produceState<ClipResult?>(null, query, attempt) {
        value = null
        value = vm.resolveClip(query, refresh = attempt > 0)
    }

    if (useSearchPage) {
        ClipSearchScreen(query, onClose)
        return
    }

    when (val r = result) {
        null -> ClipMessage("Buscando el videoclip…", "", loading = true, onClose = onClose) {}
        is ClipResult.Found -> key(r.ids) {
            EmbeddedClip(
                ids = r.ids, query = query, onClose = onClose,
                onSearchAgain = { attempt++ },
                onUseSearchPage = { useSearchPage = true },
            )
        }
        ClipResult.NoKey -> ClipMessage(
            "Falta la clave de YouTube",
            "Para ver los videoclips dentro de la app, a pantalla completa, añade tu clave gratuita de la API de YouTube en Ajustes → Videoclips de YouTube.",
            loading = false, onClose = onClose,
        ) {
            Button(onClick = onOpenSettings) { Text("Ir a Ajustes") }
            OutlinedButton(onClick = { useSearchPage = true }) { Text("Buscar en YouTube (navegador interno)") }
        }
        ClipResult.Offline -> ClipMessage(
            "Sin conexión a Internet",
            "Los videoclips necesitan conexión; el resto de la app funciona sin ella.",
            loading = false, onClose = onClose,
        ) { Button(onClick = { attempt++ }) { Text("Reintentar") } }
        is ClipResult.Failed -> ClipMessage(
            "No se pudo cargar el videoclip", r.message, loading = false, onClose = onClose,
        ) {
            Button(onClick = { attempt++ }) { Text("Reintentar") }
            OutlinedButton(onClick = { useSearchPage = true }) { Text("Buscar en YouTube (navegador interno)") }
        }
    }
}

@Composable
private fun ClipMessage(
    title: String,
    text: String,
    loading: Boolean,
    onClose: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    Column(
        Modifier.fillMaxSize().background(Color.Black).blockTouches().windowInsetsPadding(WindowInsets.safeDrawing).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        if (loading) CircularProgressIndicator(color = Color.White)
        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
        if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        actions()
        TextButton(onClick = onClose) { Text("Cerrar", color = Color.White) }
    }
}

/** Puente JS → Kotlin: estado del reproductor de YouTube y errores. */
private class ClipBridge(
    private val onStateChanged: (Int) -> Unit,
    private val onPlayerError: (Int) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onReady() = Unit

    @JavascriptInterface
    fun onState(state: Int) { main.post { onStateChanged(state) } }

    @JavascriptInterface
    fun onError(code: Int) { main.post { onPlayerError(code) } }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedClip(
    ids: List<String>,
    query: String,
    onClose: () -> Unit,
    onSearchAgain: () -> Unit,
    onUseSearchPage: () -> Unit,
) {
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(0) }
    var fill by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val ratioLabel = remember { screenRatioLabel(context) }

    ImmersiveMode()
    BackHandler(onBack = onClose)

    // Pausar el videoclip si la app pasa a segundo plano.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) webView?.evaluateJavascript("pauseNow()", null)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Los controles se ocultan solos mientras suena.
    LaunchedEffect(controlsVisible, playing) {
        if (controlsVisible && playing) {
            delay(3000)
            controlsVisible = false
        }
    }

    val bridge = remember {
        ClipBridge(
            onStateChanged = { s ->
                playing = s == 1 || s == 3
                if (s == 0 || s == 2) controlsVisible = true
            },
            // Si ese video no se puede incrustar (error 101/150…) se prueba con el siguiente resultado.
            onPlayerError = { _ ->
                if (index < ids.lastIndex) {
                    index += 1
                } else {
                    failed = true
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black).blockTouches()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    addJavascriptInterface(bridge, "mb")
                    // La página es local: se bloquea cualquier navegación del documento principal.
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            request.isForMainFrame
                    }
                    webView = this
                }
            },
            update = { wv ->
                val id = ids[index]
                if (wv.tag != id) {
                    wv.tag = id
                    wv.loadDataWithBaseURL(BASE_URL, clipHtml(id, fill), "text/html", "utf-8", null)
                }
            },
            onRelease = { it.destroy() },
            modifier = Modifier.fillMaxSize(),
        )

        // Capa para mostrar/ocultar controles con un toque
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { controlsVisible = !controlsVisible } })

        AnimatedVisibility(visible = controlsVisible && !failed, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier.align(Alignment.TopStart).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                        .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(
                            query.removeSuffix(" official video"), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium, color = Color.White,
                        )
                        Text(
                            if (fill) "Pantalla completa · $ratioLabel" else "Ajustado al video",
                            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    TextButton(onClick = {
                        fill = !fill
                        webView?.evaluateJavascript("setMode('${if (fill) "cover" else "contain"}')", null)
                    }) { Text(if (fill) "Ajustar" else "Llenar", color = Color.White) }
                }
                Box(
                    Modifier.align(Alignment.Center).size(72.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = { webView?.evaluateJavascript("toggle()", null) }, modifier = Modifier.size(72.dp)) {
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (playing) "Pausar" else "Reproducir",
                            tint = Color.White, modifier = Modifier.size(44.dp),
                        )
                    }
                }
                TextButton(
                    onClick = { openInYoutube(context, ids[index]) },
                    modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing),
                ) { Text("Abrir en YouTube", color = Color.White) }
            }
        }

        if (failed) {
            Column(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))
                    .windowInsetsPadding(WindowInsets.safeDrawing).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                Text("Este videoclip no se puede ver dentro de la app", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                Text(
                    "YouTube no permite incrustar los videos encontrados (o ya no están disponibles).",
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center,
                )
                Button(onClick = { openInYoutube(context, ids[index]) }) { Text("Abrir en YouTube") }
                OutlinedButton(onClick = onSearchAgain) { Text("Buscar otro videoclip") }
                OutlinedButton(onClick = onUseSearchPage) { Text("Elegir yo en YouTube") }
                TextButton(onClick = onClose) { Text("Cerrar", color = Color.White) }
            }
        }
    }
}

private fun openInYoutube(context: Context, videoId: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Oculta las barras del sistema y mantiene la pantalla encendida mientras se ve el videoclip. */
@Composable
private fun ImmersiveMode() {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Proporción real de la pantalla, p. ej. "19.5:9" o "20:9". */
private fun screenRatioLabel(context: Context): String {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val (w, h) = if (android.os.Build.VERSION.SDK_INT >= 30) {
        val b = wm.maximumWindowMetrics.bounds
        b.width() to b.height()
    } else {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        dm.widthPixels to dm.heightPixels
    }
    val ratio = max(w, h).toFloat() / min(w, h).coerceAtLeast(1) * 9f
    return String.format(java.util.Locale.US, "%.1f", ratio).removeSuffix(".0") + ":9"
}

/**
 * Página con el reproductor oficial de YouTube (IFrame API). El video se escala para cubrir la pantalla
 * ("cover", recortando lo que sobre) o para verse entero ("contain"). Sin controles propios de YouTube.
 */
private fun clipHtml(videoId: String, fill: Boolean): String {
    val mode = if (fill) "cover" else "contain"
    return """
<!DOCTYPE html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
html,body{margin:0;height:100%;background:#000;overflow:hidden}
#player{position:fixed;left:50%;top:50%;transform:translate(-50%,-50%);border:0;pointer-events:none}
</style></head><body>
<div id="player"></div>
<script>
var player=null, mode='$mode', AR=16/9;
function layout(){
  var vw=window.innerWidth, vh=window.innerHeight, h;
  if(mode==='cover'){h=Math.max(vh,vw/AR);}else{h=Math.min(vh,vw/AR);}
  var el=document.getElementById('player');
  el.style.height=h+'px'; el.style.width=(h*AR)+'px';
}
window.addEventListener('resize',layout); layout();
function onYouTubeIframeAPIReady(){
  player=new YT.Player('player',{
    host:'https://www.youtube-nocookie.com', videoId:'$videoId',
    playerVars:{autoplay:1,controls:0,playsinline:1,rel:0,modestbranding:1,fs:0,iv_load_policy:3,disablekb:1,origin:'https://com.reproductor.app'},
    events:{
      onReady:function(e){layout();e.target.playVideo();mb.onReady();},
      onStateChange:function(e){mb.onState(e.data);},
      onError:function(e){mb.onError(e.data);}
    }
  });
  layout();
}
function toggle(){if(!player||!player.getPlayerState)return;if(player.getPlayerState()===1){player.pauseVideo();}else{player.playVideo();}}
function pauseNow(){if(player&&player.pauseVideo){player.pauseVideo();}}
function setMode(m){mode=m;layout();}
</script>
<script src="https://www.youtube.com/iframe_api"></script>
</body></html>
""".trimIndent()
}
