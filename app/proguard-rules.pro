# meticulous and honest
# R8 / ProGuard Optimization Rules for VoidsMood
# Tailored for low-latency, 120Hz smooth rendering on ARMv7 (Infinix Smart 9 HD) and modern devices.

-repackageclasses ''
-allowaccessmodification

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# AndroidX Compose
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# Android Lifecycle & ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Android Native Callbacks
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-dontwarn org.conscrypt.**
-dontwarn okio.**
