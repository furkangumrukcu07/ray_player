# ===================================================================
# Android & Kotlin Standard Rules
# ===================================================================
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Keep native methods and JNI bindings
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable CREATORs
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===================================================================
# Kotlinx Serialization
# ===================================================================
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# ===================================================================
# Room Database
# ===================================================================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.EntityDeletionOrUpdateAdapter { *; }
-keep class * extends androidx.room.EntityInsertionAdapter { *; }
-keep class com.ray.iptv.data.local.** { *; }
-keep class com.ray.iptv.data.model.** { *; }

# ===================================================================
# MPV Android Library & Native JNI
# ===================================================================
-keep class is.xyz.mpv.** { *; }
-keepclassmembers class is.xyz.mpv.** { *; }

# ===================================================================
# Media3 / ExoPlayer
# ===================================================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ===================================================================
# OkHttp & Coroutines
# ===================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ===================================================================
# Google Play Billing & Firebase
# ===================================================================
-keep class com.android.billingclient.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ===================================================================
# Coil Image Loader
# ===================================================================
-keep class coil.** { *; }
-dontwarn coil.**

