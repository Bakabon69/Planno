# ============================================================================
# Planno Enterprise Code Hardening & Obfuscation ProGuard Rules
# ============================================================================

# 1. Aggressive Obfuscation & Renaming
-repackageclasses ''
-allowaccessmodification
-overloadaggressively
-dontusemixedcaseclassnames

# 2. Strip Source File Names, Line Numbers & Debug Attributes
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable

# 3. Strip Logging & Console Dumps in Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 4. Protect Android Core Lifecycle Components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 5. Kotlin Serialization Rules
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static <fields>;
    public <methods>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# 6. Google Gson Data Models
-keepclassmembers class com.example.taskmanager.data.TaskDTO { <fields>; }
-keepclassmembers class com.example.taskmanager.settings.model.** { <fields>; }

# 7. Jetpack Compose
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# 8. Cryptographic Vault Protection
-keep class com.example.taskmanager.security.** { *; }
