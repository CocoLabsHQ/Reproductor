package com.reproductor.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.ui.App
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.hasAudioPermission
import com.reproductor.app.ui.theme.ReproductorTheme
import com.reproductor.app.ui.theme.resolveTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mode by vm.themeMode.collectAsStateWithLifecycle()
            val prefs by vm.appearance.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val theme = remember(prefs, mode, systemDark) { resolveTheme(prefs, mode, systemDark) }
            val dark = theme.dark
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            ReproductorTheme(theme) { App(vm) }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onAppStart(hasAudioPermission(this))
    }

    override fun onStop() {
        vm.onAppStop()
        super.onStop()
    }
}
