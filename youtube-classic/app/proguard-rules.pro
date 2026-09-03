# NewPipeExtractor uses Rhino to run YouTube's player JS (signature/n-param
# deciphering) - keep it intact under minification.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
