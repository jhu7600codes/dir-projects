# keep yt-dlp's jackson mappers if minify is ever turned on
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
