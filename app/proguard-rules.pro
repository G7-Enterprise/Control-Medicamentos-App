# =============================================================
#  ProGuard / R8 — Control Medicamentos
#  Reglas de ofuscación y anti-ingeniería inversa
# =============================================================

# --- Ofuscación agresiva: renombrar clases y métodos ----------
-repackageclasses 'cm'
-allowaccessmodification
-overloadaggressively

# --- Eliminar trazas de depuración del código compilado ------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# --- Ocultar nombres de archivo fuente en stack traces -------
-renamesourcefileattribute X
-keepattributes SourceFile,LineNumberTable

# =============================================================
#  Conservar lo que necesita reflexión / generación de código
# =============================================================

# Room: entidades, DAOs y la implementación generada por KSP
-keep class com.carlos.controlmedicamentos.data.** { *; }
-keep interface com.carlos.controlmedicamentos.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Kotlin: metadatos necesarios para la reflexión de coroutines y serialización
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.jvm.JvmStatic *;
    @kotlin.jvm.JvmField *;
}

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# BroadcastReceivers, Services y Activities declarados en el Manifiesto
-keep class com.carlos.controlmedicamentos.**Activity { *; }
-keep class com.carlos.controlmedicamentos.**Receiver { *; }
-keep class com.carlos.controlmedicamentos.**Service { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Security Crypto (EncryptedSharedPreferences / MasterKey)
-keep class androidx.security.crypto.** { *; }

# ML Kit (reconocimiento de texto)
-keep class com.google.mlkit.** { *; }

# OSMDroid
-keep class org.osmdroid.** { *; }

# Gson / JsonElement
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Clases serializadas con Gson (reflexión por nombre de campo)
-keep class com.carlos.controlmedicamentos.EmergencyContact { *; }
-keepclassmembers class com.carlos.controlmedicamentos.EmergencyContact { *; }

# Compose: el plugin de Compose ya genera reglas, pero añadimos seguridad extra
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Health Connect
-keep class androidx.health.connect.** { *; }

# =============================================================
#  Anti-ingeniería inversa adicional
# =============================================================

# Eliminar clases de prueba / reflexión interna de Kotlin que no son necesarias en release
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn org.jetbrains.annotations.**

# Suprimir advertencias de librerías que usan APIs internas de Android
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.*
-dontwarn javax.annotation.**
