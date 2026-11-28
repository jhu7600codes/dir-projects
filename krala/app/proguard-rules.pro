# no renaming, so crash logs stay readable
-dontobfuscate

-keep class app.krala.** { *; }

# kotlinx serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class io.github.jan.supabase.**$$serializer { *; }
-keepclassmembers class io.github.jan.supabase.** { *** Companion; }

# requery sqlite (jni)
-keep class io.requery.android.database.** { *; }

# ktor / okhttp
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**
