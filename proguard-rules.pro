# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/danlazarescu/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any specific rules for your dependencies here

# Retrofit
# Retrofit does reflection on generic types. InnerClasses is required to use Signature and EnclosingMethod.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit 2.x
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Gson
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Keep data models
-keep class green.go.model.** { *; }
-keepclassmembers class green.go.model.** { *; }

# Kotlin
-keep class kotlin.Metadata { *; }

# Keep Retrofit interfaces and classes
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions

# Keep specific Retrofit/OkHttp classes that might be used via reflection/generics
-keep class retrofit2.Response { *; }
-keep class okhttp3.ResponseBody { *; }
-keep class okhttp3.RequestBody { *; }

# Keep ApiService fully
-keep interface green.go.network.ApiService { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Kotlin Coroutines and Continuation
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Generic Utils
-keep class java.lang.reflect.ParameterizedType { *; }
