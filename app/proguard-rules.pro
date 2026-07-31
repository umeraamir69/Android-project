# LectureLens release shrinker / obfuscation keeps.
# Applied when minifyEnabled=true (release).

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_GeneratedInjector { *; }
-keep class * extends com.lecturelens.LectureLensApp { *; }

# Room entities / DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# Gson / Retrofit DTOs (reflection)
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.lecturelens.data.remote.dto.** { *; }
-keepclassmembers class com.lecturelens.data.remote.dto.** { *; }
-keepnames class com.lecturelens.data.remote.**Service { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# WorkManager workers (instantiated by class name)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Enums used in status / prefs
-keepclassmembers enum com.lecturelens.domain.model.** { *; }

# ViewBinding
-keep class com.lecturelens.databinding.** { *; }
