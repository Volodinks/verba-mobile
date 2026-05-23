# Firestore reads documents into our data classes via reflection.
# Keep model classes and their members so release builds don't break deserialization.
-keep class com.verba.mobile.data.** { *; }

# Keep Kotlin metadata for reflection-based serialization.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
