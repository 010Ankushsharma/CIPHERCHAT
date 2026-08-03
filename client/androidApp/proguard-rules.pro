# CipherChat Android ProGuard Rules

# ── Kotlin serialization ──────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.cipherchat.**$$serializer { *; }
-keepclassmembers class com.cipherchat.** {
    *** Companion;
}
-keepclasseswithmembers class com.cipherchat.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Ktor client ───────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# ── Koin ─────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# ── Voyager navigation ────────────────────────────────────────────────
-keep class cafe.adriel.voyager.** { *; }

# ── SQLDelight ───────────────────────────────────────────────────────
-keep class app.cash.sqldelight.** { *; }

# ── Libsodium bindings ───────────────────────────────────────────────
-keep class com.ionspin.kotlin.crypto.** { *; }

# ── Coil image loading ───────────────────────────────────────────────
-keep class coil3.** { *; }

# ── General Kotlin ───────────────────────────────────────────────────
-keepclassmembers class **$WhenMappings { <fields>; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── Okhttp (used by Ktor on Android) ─────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
