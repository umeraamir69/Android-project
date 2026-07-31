import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProperty(name: String, default: String = ""): String =
    localProperties.getProperty(name)?.trim().orEmpty().ifEmpty { default }

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

        // Track 4 — keys from local.properties (gitignored). Empty string if unset.
        buildConfigField("String", "STT_API_KEY", "\"${localProperty("STT_API_KEY")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperty("GEMINI_API_KEY")}\"")
        // Optional long-audio path: GCS upload + speech:longrunningrecognize
        buildConfigField("String", "GCS_BUCKET", "\"${localProperty("GCS_BUCKET")}\"")
        buildConfigField("String", "GCS_OAUTH_TOKEN", "\"${localProperty("GCS_OAUTH_TOKEN")}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Java + Views stack (Day 0 decision) — ViewBinding, no Compose.
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    lint {
        abortOnError = true
        // French (values-fr) covers course rubric strings; full parity is WIP.
        // Treat missing translations as warnings so CI stays green.
        warning += "MissingTranslation"
    }
}
dependencies {
    // UI — Views
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
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
    // Firebase — BoM pins compatible versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    testImplementation(libs.core.testing)           // Track 3: InstantTaskExecutorRule for LiveData
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}