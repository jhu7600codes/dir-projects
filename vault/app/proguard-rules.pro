# Add project specific ProGuard rules here.

# kotlinx.serialization generates a synthetic $serializer companion per @Serializable class;
# only matters once minifyEnabled is turned on for a release build, but kept here so that
# switch doesn't quietly break GameSave (de)serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.orbitalsurf.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.orbitalsurf.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
