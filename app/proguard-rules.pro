# Media3, Room y Coil incluyen sus propias reglas de consumidor.
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

# Puente JavaScript del videoclip incrustado
-keepclassmembers class com.reproductor.app.ui.screens.** { @android.webkit.JavascriptInterface <methods>; }
