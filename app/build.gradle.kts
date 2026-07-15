plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.palepal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.palepal"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // --- TAMBAHAN LIBRARY UNTUK KAMERA & AI ---

    // 1. Library CameraX (Untuk menyalakan kamera di dalam layar)
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // 2. Library Guava (Untuk mengatasi error ListenableFuture)
    implementation("com.google.guava:guava:31.1-android")

    // 3. Library TensorFlow Lite (Untuk membaca model AI YOLOv8)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
}