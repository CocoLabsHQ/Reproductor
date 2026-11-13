package com.reproductor.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Estilo elegido: tema, color de acento y, si es "custom", la imagen de galería con sus ajustes. */
data class AppearancePrefs(
    val themeId: String = "classic",
    val accent: Int = 0,            // 0 = el del tema
    val imagePath: String? = null,
    val imageVersion: Int = 0,
    val transparency: Float = 0.45f, // más alto = se ve más la imagen
    val blur: Float = 0f,
)

class SettingsStore(context: Context) {
    private val ds = context.applicationContext.dataStore
    private val themeKey = intPreferencesKey("theme_mode")
    private val lyricsKey = stringPreferencesKey("lyrics_tree_uri")
    private val themeIdKey = stringPreferencesKey("theme_id")
    private val accentKey = intPreferencesKey("accent")
    private val imagePathKey = stringPreferencesKey("image_path")
    private val imageVersionKey = intPreferencesKey("image_version")
    private val transparencyKey = floatPreferencesKey("transparency")
    private val blurKey = floatPreferencesKey("blur")
    private val hideWaKey = booleanPreferencesKey("hide_whatsapp")
    private val autoUpdateKey = booleanPreferencesKey("auto_update")
    private val lastSeenKey = stringPreferencesKey("last_seen_version")
    private val youtubeKeyKey = stringPreferencesKey("youtube_api_key")

    /** 0 = sistema, 1 = claro, 2 = oscuro (solo afecta al tema Clásico) */
    val themeMode: Flow<Int> = ds.data.map { it[themeKey] ?: 0 }
    val lyricsTree: Flow<String?> = ds.data.map { it[lyricsKey] }
    val appearance: Flow<AppearancePrefs> = ds.data.map {
        AppearancePrefs(
            themeId = it[themeIdKey] ?: "classic",
            accent = it[accentKey] ?: 0,
            imagePath = it[imagePathKey],
            imageVersion = it[imageVersionKey] ?: 0,
            transparency = it[transparencyKey] ?: 0.45f,
            blur = it[blurKey] ?: 0f,
        )
    }
    val hideWhatsApp: Flow<Boolean> = ds.data.map { it[hideWaKey] ?: false }
    val autoUpdate: Flow<Boolean> = ds.data.map { it[autoUpdateKey] ?: true }
    val lastSeenVersion: Flow<String?> = ds.data.map { it[lastSeenKey] }
    val youtubeKey: Flow<String?> = ds.data.map { it[youtubeKeyKey] }

    suspend fun setThemeMode(mode: Int) { ds.edit { it[themeKey] = mode } }
    suspend fun setLyricsTree(uri: String?) {
        ds.edit { if (uri == null) it.remove(lyricsKey) else it[lyricsKey] = uri }
    }
    suspend fun setThemeId(id: String) { ds.edit { it[themeIdKey] = id } }
    suspend fun setAccent(argb: Int) { ds.edit { it[accentKey] = argb } }
    suspend fun setTransparency(v: Float) { ds.edit { it[transparencyKey] = v } }
    suspend fun setBlur(v: Float) { ds.edit { it[blurKey] = v } }
    suspend fun setCustomImage(path: String, version: Int) {
        ds.edit { it[imagePathKey] = path; it[imageVersionKey] = version }
    }
    suspend fun setHideWhatsApp(v: Boolean) { ds.edit { it[hideWaKey] = v } }
    suspend fun setAutoUpdate(v: Boolean) { ds.edit { it[autoUpdateKey] = v } }
    suspend fun setLastSeenVersion(v: String) { ds.edit { it[lastSeenKey] = v } }
    suspend fun setYoutubeKey(key: String?) {
        ds.edit { if (key == null) it.remove(youtubeKeyKey) else it[youtubeKeyKey] = key }
    }
}
