# kotlinx.serialization: keep generated serializers for backup models
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.rayaurelius.studybuddy.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rayaurelius.studybuddy.**$$serializer { *; }
