plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize") // Ajout du plugin kotlin-parcelize
}

android {
    namespace = "com.pastaga.geronimo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pastaga.geronimo"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
    }
}

dependencies {
    // Dépendances pour les services de localisation Google Play
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ... vos dépendances existantes du nouveau projet ...

    // Coil for image loading
    implementation("io.coil-kt:coil:2.4.0")

    // Retrofit for API communication
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp for logging (debug only)
    debugImplementation("com.squareup.okhttp3:logging-interceptor:4.11.0")


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

    // Importez la Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    // Ajoutez les dépendances Firebase que vous utilisez dans votre application
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")

    // Dépendances pour Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0") // Vérifiez la dernière version stable
    
    // Google Play Billing (Phase 3)
    implementation("com.android.billingclient:billing-ktx:6.2.1")
    
    // Lottie pour les animations
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")
}