plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.lecturelens"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lecturelens"
        minSdk = 29                // Android 10+, per architecture doc §1.2
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Java + Views stack (Day 0 decision) — ViewBinding, no Compose.
    buildFeatures { viewBinding = true }
}

dependencies {
    // UI — Views
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Lifecycle — ViewModel + LiveData (no coroutines)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)

    // Room (Track 1 fills in entities/DAOs in week 1)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // WorkManager (Tracks 3 + 4)
    implementation(libs.work.runtime)

    // Hilt — Java annotation processing
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    // Networking (Track 4 fills in services)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Playback (Track 5)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // EncryptedSharedPreferences (Track 1 Auth)
    implementation(libs.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
