# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations for reflection
-keepattributes *Annotation*

# ===== Retrofit & OkHttp =====
# Retrofit interfaces and models
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===== Gson =====
# Gson uses generic type information stored in a class file when working with fields.
-keepattributes Signature

# Keep generic signature of Call, Response (R8 full mode strips signatures)
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Keep all data classes used for JSON serialization/deserialization
-keep class com.pastaga.geronimo.ImageAnalyzer$AnalyzeImageRequest { *; }
-keep class com.pastaga.geronimo.ImageAnalyzer$AnalyzeImageResponse { *; }
-keep class com.pastaga.geronimo.AnalysisEntry { *; }

# ===== Firebase =====
# Firebase already has its own ProGuard rules included in the libraries
# but we ensure critical classes are kept
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ===== Kotlin Coroutines =====
# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== UCrop =====
-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

# ===== Coil =====
-dontwarn coil.**
-keep class coil.** { *; }

# ===== Play Services =====
# Keep Google Play Services classes
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**