<p align="center">
  <img src="app/src/main/res/drawable-nodpi/logo.png" alt="Reproductor" width="220">
</p>

<h1 align="center">Reproductor</h1>

<p align="center">
  Un reproductor de música y multimedia para Android: ligero, bonito y sin anuncios.<br>
  <b>Sin publicidad · Sin rastreadores · La música funciona 100 % offline</b>
</p>

---

## ¿Qué es?

**Reproductor** es una app Android nativa (Kotlin + Jetpack Compose) que reproduce la música, los videos y los GIF que ya tienes en tu teléfono. Su identidad visual sale de su mascota, el gallo con auriculares: turquesa y cian de fondo, rojo gallo para los botones principales y naranja y amarillo para los detalles.

> Versión 1.2.1. Primera versión pública: el código compila y se genera el APK automáticamente, pero se está probando en dispositivos reales. Si encuentras un fallo, abre un *issue*.

## Funciones

### 🎵 Reproducción
- Reproducir, pausar, anterior y siguiente.
- Barra de progreso interactiva y control de volumen.
- Aleatorio y tres modos de repetición: ninguna, toda la lista o la canción actual.
- Portada del álbum, título, artista, álbum y duración.
- **Cola de reproducción editable**: reordena arrastrando, quita canciones, añade "a continuación" o al final.
- Formatos habituales: MP3, AAC/M4A, FLAC, OGG, WAV, Opus y más (motor ExoPlayer / Media3).

### 📚 Biblioteca
- Escanea la música del dispositivo y se actualiza sola cuando añades archivos.
- Navega por **canciones, artistas, álbumes, carpetas, géneros** (si hay metadatos) y **añadidas recientemente**.
- **Búsqueda** por título, artista, álbum, género o nombre de archivo, ignorando acentos.
- "Quitar de la biblioteca" solo oculta la canción en la app: **nunca borra el archivo**. Se puede restaurar desde Ajustes.

### 📋 Playlists
- Crear, renombrar y eliminar playlists.
- Añadir y quitar canciones, y **reordenarlas arrastrando**.
- Se conservan al cerrar la app o reiniciar el teléfono.
- **Selección múltiple:** mantén pulsada una canción y toca las demás (o usa "Todo") para añadirlas a una playlist, a la cola o quitarlas de la biblioteca de una vez.

### 🎬 Video y GIF
- Reproductor de video integrado: reproducir/pausar, avanzar/retroceder, barra de progreso y volumen.
- Visor de **GIF animados**.
- Si un video o GIF tiene el mismo nombre que una canción, se ofrece al reproducirla.

### 🎤 Letras
- Lee archivos **.lrc** (sincronizados línea a línea, con salto al tocar una línea) y **.txt**.
- Funciona sin Internet: busca junto a la canción (Android 10 o anterior) o en la carpeta de letras que elijas en Ajustes (Android 11 o superior).
- Si no hay letra, lo indica sin más.

### 🔔 Segundo plano
- Sigue sonando al minimizar la app, cambiar de aplicación o apagar la pantalla.
- Controles en la **notificación** y en la **pantalla de bloqueo**, y compatibilidad con botones de auriculares y Bluetooth.
- Se pausa solo si desconectas los auriculares.

### 🎨 Interfaz y estilos
- Diseño minimalista con formas redondeadas y animaciones suaves.
- **Estilos:** 9 temas prediseñados (Clásico, Atardecer, Bosque, Océano, Neón, Medianoche, Aurora, Caramelo y Menta) o **una imagen de tu galería** con ajustes de color, transparencia y desenfoque.
- Modo claro, oscuro o según el sistema (tema Clásico).
- La cabecera de Inicio (con el gallo) se oculta sola y combina con el tema elegido.
- Mini reproductor persistente mientras navegas.
- Pantallas: Inicio, Biblioteca, Playlists, Reproductor, Búsqueda y Ajustes.

### 📊 Resumen de música
- Resumen mensual de lo que escuchas: minutos, reproducciones, canciones y artistas favoritos.
- Tras **un año de registro**, un resumen anual con diapositivas animadas al estilo "Wrapped".
- Videoclips de tus canciones favoritas dentro de la app (solo cuando tú los pides; usa Internet).
- El registro se guarda solo en tu teléfono.

### 🔄 Actualizaciones
- Aviso dentro de la app cuando hay una versión nueva, con su tamaño y la opción "Más tarde".
- Tras actualizar, una ventana con las novedades.

### 🙈 Ocultar audios de WhatsApp
- Opción en Ajustes: se detectan por la carpeta de WhatsApp y por el nombre del archivo (`AUD-…-WA0001`).

## Privacidad
- Sin anuncios, sin SDK publicitarios, sin analíticas ni rastreadores.
- La música, las playlists, las letras y los resúmenes funcionan **sin conexión**.
- Internet se usa solo en dos casos: **ver un videoclip** (cuando tú lo pides; carga YouTube dentro de la app) y **buscar actualizaciones** (consulta las Releases de GitHub; se puede desactivar en Ajustes).
- Pide acceso a música, videos e imágenes (para encontrar tus archivos) y notificaciones (para los controles).

## Instalación
1. Ve a **Releases** y descarga el último `app-release.apk`.
2. Ábrelo en el teléfono y permite "instalar apps desconocidas" cuando Android lo pida.
3. Al iniciar, concede el permiso para leer tus archivos multimedia.

Requiere **Android 8.0 (API 26)** o superior.

## Compilar desde el código
La forma más fácil: sube el repositorio a GitHub y descarga el APK desde **Actions** (el flujo de trabajo ya está incluido).

En local, con JDK 17 y el Android SDK:
```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleRelease
```
El APK queda en `app/build/outputs/apk/release/`. También puedes abrir la carpeta con Android Studio y usar *Build → Build APK(s)*.

## Tecnologías
Kotlin · Jetpack Compose · Material 3 · AndroidX Media3 (ExoPlayer y MediaSession) · Room · DataStore · Coil · Navigation Compose.

## Estructura
```
app/src/main/java/com/reproductor/app/
  data/       MediaStore, Room, DataStore, letras y portadas
  domain/     Lector de archivos LRC
  playback/   Servicio de reproducción y controlador
  ui/         Tema, navegación, pantallas y componentes
```

## Limitaciones conocidas
- No lee letras incrustadas en las etiquetas de los archivos, solo `.lrc` y `.txt`.
- En Android 11 o superior las letras requieren elegir una carpeta una vez.
- La portada de la notificación puede no aparecer con algunos archivos.
- Los videoclips se buscan en YouTube y tú eliges el resultado; la app no puede reproducir uno automáticamente.
- Los temas prediseñados están dibujados por código (degradados y formas), no son imágenes.
- Para publicar en Google Play hay que firmar con tu propia clave (ver abajo).

## Publicar una versión (para que las apps instaladas reciban el aviso)
1. Sube la versión en `app/build.gradle.kts` (`versionCode` y `versionName`) y escribe las novedades en `app/src/main/assets/whatsnew.txt`.
2. `git tag v1.2.0 && git push origin v1.2.0`. GitHub compila y publica la Release con el APK.

Para que las actualizaciones se puedan instalar encima de la versión anterior, el APK debe firmarse **siempre con la misma clave**: guárdala como secretos del repositorio (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) y conserva una copia del `.jks`.
