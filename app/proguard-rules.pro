# Keep OpenCV
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.xiaoyuezhu.app.**$$serializer { *; }
-keepclassmembers class com.xiaoyuezhu.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.xiaoyuezhu.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
