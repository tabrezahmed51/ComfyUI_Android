# ==============================================================================
# Proguard & R8 Rules for ComfyUI Mobile (Enterprise Production Grade)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. General Reflection, Metadata & Annotations Preservation
# ------------------------------------------------------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes *Annotation*, Exceptions

# ------------------------------------------------------------------------------
# 2. Retrofit 2, OkHttp 3 & Okio Networking
# ------------------------------------------------------------------------------
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep class com.example.comfyui.data.network.api.** { *; }
-keep class retrofit2.converter.gson.** { *; }
-keep class retrofit2.converter.scalars.** { *; }

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okhttp3.logging.** { *; }
-keep class okio.** { *; }

-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ------------------------------------------------------------------------------
# 3. Gson Serialization & Domain / Network Data Models
# ------------------------------------------------------------------------------
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# Preserve all data classes used in networking and workflow serialization
-keep class com.example.comfyui.data.network.model.** { *; }
-keepclassmembers class com.example.comfyui.data.network.model.** { <fields>; <methods>; }
-keep class com.example.comfyui.data.model.** { *; }
-keepclassmembers class com.example.comfyui.data.model.** { <fields>; <methods>; }

# ------------------------------------------------------------------------------
# 4. Firebase Firestore, Authentication & Google Play Services
# ------------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }

# Firestore model serialization requirements (fields, no-arg constructors, getters/setters)
-keepclassmembers class * {
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
}

-keep class com.example.comfyui.data.firebase.** {
    <init>();
    <fields>;
    <methods>;
}
-keepclassmembers class com.example.comfyui.data.firebase.** { *; }

# Google Credential Manager & Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# 5. Kotlin Coroutines & Flow
# ------------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.android.** { *; }
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 6. Coil Image Loading Library
# ------------------------------------------------------------------------------
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ------------------------------------------------------------------------------
# 7. Jetpack Compose & AndroidX Lifecycle
# ------------------------------------------------------------------------------
-keep class com.example.comfyui.ui.viewmodel.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }
