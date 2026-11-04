# F.R.I.D.A.Y. ProGuard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.friday.assistant.data.remote.models.** { *; }
-keep class com.friday.assistant.data.local.entity.** { *; }

