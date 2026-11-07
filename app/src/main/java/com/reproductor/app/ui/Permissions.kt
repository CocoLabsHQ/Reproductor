package com.reproductor.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

/** Solo el permiso de audio es imprescindible; video, GIF y notificaciones son opcionales. */
fun hasAudioPermission(context: Context): Boolean {
    val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
}
