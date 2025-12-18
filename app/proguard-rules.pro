# ========== CONFIGURACIÓN BÁSICA ==========
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ========== KOTLIN ==========
-keepattributes *Annotation*
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# ========== FIREBASE ==========
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes Signature, *Annotation*

# ========== ROOM DATABASE ==========
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# ========== MAPBOX ==========
-keep class com.mapbox.** { *; }
-dontwarn com.mapbox.**

# ========== COROUTINES ==========
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ========== VIEWMODEL/LIFECYCLE ==========
-keep class * extends androidx.lifecycle.ViewModel

# ========== NAVIGATION ==========
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ========== COMPOSE ==========
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ========== FIREBASE AUTH ==========
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ========== ROOM QUERIES ==========
-keepclassmembers class * {
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.Query *;
}

# ========== SERIALIZATION ==========
-keepattributes InnerClasses
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========== HILT ==========
-keep class com.google.dagger.hilt.** { *; }
-dontwarn com.google.dagger.hilt.**