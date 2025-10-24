plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.uniremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.uniremote"
        minSdk = 26
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
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    // Amazon Fling SDK (local JAR). Drop amazon-fling.jar into app/libs/ to enable.
    val flingJar = file("libs/amazon-fling.jar")
    if (flingJar.exists()) {
        implementation(files(flingJar))
    } else {
        logger.warn("amazon-fling.jar not found in app/libs — Fire TV Fling features will be disabled at compile time.")
    }
    // MediaRouter for FlingMediaRouteProvider integration (device discovery/control via routes)
    implementation("androidx.mediarouter:mediarouter:1.3.1")
    debugImplementation(libs.androidx.compose.ui.tooling)
}