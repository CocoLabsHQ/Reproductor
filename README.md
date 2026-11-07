# Reproductor 🐓🎧

Reproductor multimedia Android nativo (Kotlin + Jetpack Compose + Media3). Sin anuncios, sin rastreadores y **sin permiso de Internet**.

## Funciones
- Música: reproducir/pausar, anterior/siguiente, barra de progreso, volumen, aleatorio, repetir (ninguna / playlist / canción), portadas, cola editable (arrastrar para reordenar).
- Biblioteca (MediaStore): canciones, artistas, álbumes, carpetas, géneros, añadidas recientemente, videos y GIF. Se actualiza sola mientras la app está abierta.
- Búsqueda por título, artista, álbum, género o nombre de archivo (ignora acentos).
- Playlists persistentes (Room), con reordenación manual. "Quitar de la biblioteca" solo oculta: **nunca borra el archivo**.
- Segundo plano: servicio Media3 con notificación, pantalla de bloqueo y botones de auriculares.
- Letras `.lrc` (sincronizadas) y `.txt`, buscadas junto a la canción (Android ≤10) o en una carpeta que eliges en Ajustes (Android 11+).
- Video integrado (ExoPlayer) y GIF animados. Si un video o GIF tiene el mismo nombre que una canción, se muestra al reproducirla.
- Modo claro/oscuro/sistema. Mini reproductor persistente.

## Estructura
```
app/src/main/java/com/reproductor/app/
  data/       MediaStore, Room, DataStore, letras, portadas
  domain/     LrcParser
  playback/   PlaybackService (Media3) y PlayerController
  ui/         tema, App (navegación), MainViewModel, components/, screens/
```

## Compilar

### Opción A: en GitHub (sin instalar nada)
1. Sube el proyecto (ver comandos abajo).
2. En GitHub: pestaña **Actions** → "Compilar APK" → entra en la última ejecución.
3. Descarga el artefacto `Reproductor-release-apk` (más ligero) o `Reproductor-debug-apk`.

### Opción B: Android Studio
`File → Open` en la carpeta del proyecto, espera la sincronización y usa `Build → Build APK(s)`.

### Opción C: terminal
Requiere JDK 17 y el Android SDK (`ANDROID_HOME`). El proyecto no incluye `gradle-wrapper.jar`; se genera una vez con Gradle instalado:
```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug      # Windows: gradlew.bat assembleDebug
./gradlew assembleRelease
```
APK generados: `app/build/outputs/apk/debug/app-debug.apk` y `app/build/outputs/apk/release/app-release.apk`.

Instalar en el teléfono (depuración USB activada):
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Firmar con tu propia clave (para Play Store)
```bash
keytool -genkey -v -keystore reproductor.jks -keyalg RSA -keysize 2048 -validity 10000 -alias reproductor
```
Luego sustituye `signingConfig = signingConfigs.getByName("debug")` en `app/build.gradle.kts` por tu configuración. Nunca subas el `.jks` a GitHub (ya está en `.gitignore`).
