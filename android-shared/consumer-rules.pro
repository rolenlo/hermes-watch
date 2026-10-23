# kotlinx.serialization: keep generated serializers for the wire models
# (only matters if a consuming app turns on minification).
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.hermeswatch.shared.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hermeswatch.shared.**$$serializer { *; }
