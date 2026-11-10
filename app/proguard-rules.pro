# Proguard rules for ComfyUI Mobile (Enterprise Production Grade)

# Retain Retrofit and OkHttp annotations and interfaces
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations

-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Retain Gson Models & Serialized Fields
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.comfyui.data.network.model.** { *; }
-keep class com.example.comfyui.data.model.** { *; }

# Retain Firebase Firestore Data Models
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keep class com.example.comfyui.data.firebase.** { *; }

# Retain Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
