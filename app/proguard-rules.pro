-keep class id.satria.launcher.data.** { *; }
-keepclassmembers class id.satria.launcher.data.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }

# Coil — hanya yang dipakai
-keep class coil.** { *; }
-dontwarn coil.**

# Coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Compose: strip debug info di release (sudah di-handle R8 tapi eksplisit lebih aman)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Hapus semua Log.* call di release — zero overhead logging
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
}
