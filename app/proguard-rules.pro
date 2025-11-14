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
-keep class com.pastaga.geronimo.AnalysisHistoryManager { *; }

# Keep specific members of AnalysisEntry for Gson deserialization
-keepclassmembers class com.pastaga.geronimo.AnalysisEntry {
    <fields>;
    <methods>;
}

# Keep specific members of AnalysisHistoryManager for Gson deserialization
-keepclassmembers class com.pastaga.geronimo.AnalysisHistoryManager {
    <fields>;
    <methods>;
}

# Ensure Gson TypeToken preserves generic type information
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }

# ===== Firebase =====
# Firebase already has its own ProGuard rules included in the libraries
# but we ensure critical classes are kept
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep references to all fields of the R class
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep all public methods and constructors of custom Application, Activity, Fragment, View, and Service classes
# This is crucial for app components that are instantiated by the Android system or via reflection.
-keep public class * extends android.app.Application { public <init>(...); }
-keep public class * extends android.app.Activity { public <init>(...); }
-keep public class * extends android.app.Fragment { public <init>(...); }
-keep public class * extends androidx.fragment.app.Fragment { public <init>(...); }
-keep public class * extends android.view.View { public <init>(...); }
-keep public class * extends android.app.Service { public <init>(...); }
-keep public class * extends android.content.BroadcastReceiver { public <init>(...); }
-keep public class * extends android.content.ContentProvider { public <init>(...); }

# Keep custom views that extend from Android's View classes and are used in XML layouts
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep all classes that are used in the Android Manifest
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Keep the names of classes that are referenced by the manifest
-keep public class com.pastaga.geronimo.** { *; }

# Keep specific classes for reflection that might be needed by libraries
-keepnames class com.google.android.gms.location.FusedLocationProviderClient
-keepnames class com.yalantis.ucrop.UCrop

# Keep custom attributes
-keep class * implements android.os.Parcelable { *; }

# Keep enums
-keepnames enum * { *; }

# Keep interfaces
-keepnames interface * { *; }

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