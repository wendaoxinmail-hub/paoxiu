# Keep data models for sync
-keep class com.wendao.run.core.network.model.** { *; }

# Baidu Map / Location SDK
-keep class com.baidu.** { *; }
-keep class vi.com.** { *; }
-dontwarn com.baidu.**

# Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
