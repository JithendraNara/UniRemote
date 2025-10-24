# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep DataStore classes
-keep class androidx.datastore.** { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Amazon Fling SDK classes (if present)
-keep class com.amazon.whisperplay.** { *; }
-keep class com.amazon.whisperlink.** { *; }
-dontwarn com.amazon.whisperplay.**
-dontwarn com.amazon.whisperlink.**

# Keep Apache Thrift classes (used by Amazon Fling SDK)
-keep class org.apache.thrift.** { *; }
-dontwarn org.apache.thrift.**

# Keep MediaRouter classes
-keep class androidx.mediarouter.** { *; }

# Keep UniRemote specific classes
-keep class com.jithendranara.uniremote.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}