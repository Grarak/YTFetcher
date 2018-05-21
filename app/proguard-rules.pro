-keepattributes SourceFile,LineNumberTable
-keep class com.grarak.ytfetcher.** { *; }
-keep public class com.sothree.slidinguppanel.** { *; }
-keep public class com.google.gson.reflect.TypeToken { *; }
-keep class com.google.android.exoplayer2.** { *; }

-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
