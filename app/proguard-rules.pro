# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Retrofit interfaces
-keep interface retrofit2.Call { *; }
-keep interface retrofit2.http.Body { *; }
-keep interface retrofit2.http.Field { *; }
-keep interface retrofit2.http.FieldMap { *; }
-keep interface retrofit2.http.FormUrlEncoded { *; }
-keep interface retrofit2.http.GET { *; }
-keep interface retrofit2.http.HTTP { *; }
-keep interface retrofit2.http.Multipart { *; }
-keep interface retrofit2.http.POST { *; }
-keep interface retrofit2.http.PUT { *; }
-keep interface retrofit2.http.PATCH { *; }
-keep interface retrofit2.http.HEAD { *; }
-keep interface retrofit2.http.DELETE { *; }
-keep interface retrofit2.http.OPTIONS { *; }
-keep interface retrofit2.http.Part { *; }
-keep interface retrofit2.http.PartMap { *; }
-keep interface retrofit2.http.Path { *; }
-keep interface retrofit2.http.Query { *; }
-keep interface retrofit2.http.QueryMap { *; }
-keep interface retrofit2.http.Header { *; }
-keep interface retrofit2.http.HeaderMap { *; }
-keep interface retrofit2.http.Url { *; }

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }