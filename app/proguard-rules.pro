# Add project specific ProGuard rules here.
-keep class com.cohors.app.data.remote.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass *;
}
