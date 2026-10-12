# Gson (de)serializes the API request/response models by reflection; keep their field names.
-keep class io.github.kloeggaa.tickoff.api.models.** { *; }
# Retrofit service interfaces are proxied at runtime.
-keep,allowobfuscation interface io.github.kloeggaa.tickoff.api.*Service
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
