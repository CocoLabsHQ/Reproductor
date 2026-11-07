package com.reproductor.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {
    private val ds = context.applicationContext.dataStore
    private val themeKey = intPreferencesKey("theme_mode")
    private val lyricsKey = stringPreferencesKey("lyrics_tree_uri")

    /** 0 = sistema, 1 = claro, 2 = oscuro */
    val themeMode: Flow<Int> = ds.data.map { it[themeKey] ?: 0 }
    val lyricsTree: Flow<String?> = ds.data.map { it[lyricsKey] }

    suspend fun setThemeMode(mode: Int) { ds.edit { it[themeKey] = mode } }
    suspend fun setLyricsTree(uri: String?) {
        ds.edit { if (uri == null) it.remove(lyricsKey) else it[lyricsKey] = uri }
    }
}
