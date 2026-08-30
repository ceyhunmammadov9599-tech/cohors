# ============================================================
# Cohors — ProGuard / R8 Rules
# Production obfuscation & shrink configuration
# ============================================================

# ------------------------------------------------------------
# 1. Moshi — JSON serialization
# ------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keepclassmembers class **JsonAdapter {
    *** newInstance(...);
}

# ------------------------------------------------------------
# 2. Retrofit & OkHttp — HTTP client
# ------------------------------------------------------------
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okhttp3.** { *; }

# Platform calls to Java methods via reflection
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ------------------------------------------------------------
# 3. Hilt — Dependency Injection
# ------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep,allowobfuscation,allowshrinking class dagger.hilt.*.Hilt_* { *; }
-keep,allowobfuscation,allowshrinking class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ------------------------------------------------------------
# 4. Coroutines
# ------------------------------------------------------------
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.coroutines.**

# ------------------------------------------------------------
# 5. Room — Database
# ------------------------------------------------------------
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ------------------------------------------------------------
# 6. Coil — Image loading
# ------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ------------------------------------------------------------
# 7. App DTOs — API models must not be obfuscated
# ------------------------------------------------------------
-keep class com.cohors.app.data.remote.model.** { *; }
-keepclassmembers class com.cohors.app.data.remote.model.** { *; }

# ------------------------------------------------------------
# 8. App Hilt modules — DI providers
# ------------------------------------------------------------
-keep class com.cohors.app.di.** { *; }

# ------------------------------------------------------------
# 9. Kotlin metadata (Kotlin 2.0+)
# ------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# ------------------------------------------------------------
# 10. General Android
# ------------------------------------------------------------
-keep class android.app.Application { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends androidx.fragment.app.FragmentActivity { *; }

# ------------------------------------------------------------
# 11. Accompanist
# ------------------------------------------------------------
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ------------------------------------------------------------
# 12. Compose
# ------------------------------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ------------------------------------------------------------
# 13. Navigation
# ------------------------------------------------------------
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**
