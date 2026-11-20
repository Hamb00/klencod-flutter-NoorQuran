# kotlinx.serialization (نماذج الـ API ومسارات التنقل)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.alquran.noor.**$$serializer { *; }
-keepclassmembers class com.alquran.noor.** { *** Companion; }
-keepclasseswithmembers class com.alquran.noor.** { kotlinx.serialization.KSerializer serializer(...); }
