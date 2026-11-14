-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod
-keep class org.evoionosp.abstrak.data.**.dto.** { *; }
-keepclassmembers,allowobfuscation class org.evoionosp.abstrak.data.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
