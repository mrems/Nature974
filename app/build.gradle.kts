plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.naturepei"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.naturepei"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ... vos dépendances existantes du nouveau projet ...

    // Coil for image loading
    implementation("io.coil-kt:coil:2.4.0")

    // Retrofit for API communication
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp for logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")


    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // Activity KTX for ActivityResultContracts
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.fragment:fragment-ktx:1.6.1")

    // Material Components for Android themes and UI elements
    implementation("com.google.android.material:material:1.12.0")

    // Test Dependencies
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose BOM (Bill of Materials) pour gérer les versions des dépendances Compose
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Image Cropper Library
    // implementation("com.theartofdev.edmodo:android-image-cropper:2.3.1") // Dépendance précédente
    implementation("com.github.yalantis:ucrop:2.2.8") // Nouvelle dépendance uCrop
}