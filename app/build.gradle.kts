plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aurora"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.aurora.v9"
        minSdk = 29
        targetSdk = 36
        versionCode = 90000
        versionName = "9.0.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { jniLibs.useLegacyPackaging = false; resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

// Exporta o schema do Room (app/schemas/) para validar migrações futuras.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.room:room-runtime:2.8.2")
    implementation("androidx.room:room-ktx:2.8.2")
    ksp("androidx.room:room-compiler:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // Runtime local GGUF: llama.cpp pré-compilado para Android arm64-v8a.
    implementation("dev.ffmpegkit-maintained:llama-android:0.1.1")
}
