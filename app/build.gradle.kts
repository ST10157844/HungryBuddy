
plugins {
    alias(libs.plugins.android.application)
 alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.hungrybuddy"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.hungrybuddy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        // Traditional Android XML layouts
        viewBinding = true
    }
}

dependencies {

	// Room - local offline favourites database
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)

    // Traditional Android Views
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Activity support and ViewModel delegates
    implementation("androidx.activity:activity-ktx:1.10.1")

    // Android Core
    implementation(libs.androidx.core.ktx)

    // Lifecycle and ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-process:2.9.4")

    // Kotlin Coroutines and StateFlow support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Retrofit - TheMealDB REST API
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // Glide - Recipe images in XML ImageViews
    implementation("com.github.bumptech.glide:glide:4.16.0")

   // Firebase Authentication and Cloud Firestore
implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

// Await Firebase Tasks using Kotlin Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // WorkManager - reliable background synchronisation
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    // Unit testing
    testImplementation(libs.junit)

    // Android instrumentation testing
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
