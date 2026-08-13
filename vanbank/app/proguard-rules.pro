# Add project specific ProGuard rules here.
# minifyEnabled is off for this build (see app/build.gradle.kts); kept as a
# starting point if that's ever flipped on.
-keepattributes *Annotation*
-keep class com.vanbank.app.data.local.entity.** { *; }
