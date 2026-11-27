// meticulous and honest
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val releaseKeystoreFile = file("keystore/release.keystore")
val hasReleaseSigning = releaseKeystoreFile.exists() || System.getenv("STORE_PASSWORD") != null

android {
    namespace = "moe.rukamori.voidsmood"
    compileSdk = 35

    defaultConfig {
        applicationId = "moe.rukamori.voidsmood"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    splits {
        abi {
            isEnable = true
            isReset = true
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            if (releaseKeystoreFile.exists()) {
                storeFile = releaseKeystoreFile
                storePassword = System.getenv("STORE_PASSWORD") ?: "voidsmood123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "voidsmood"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "voidsmood123"
            } else if (System.getenv("STORE_PASSWORD") != null) {
                storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore/release.keystore")
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Strict R8 Optimization as instructed: zero lag, 60/120Hz smooth, stripped debug overhead
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Match release optimization behavior if required, but default to minimal debug suffix
            applicationIdSuffix = ".debug"
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.activity)
    implementation(libs.navigation)

    // Compose Core & Material 3 Expressive
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.material3)

    // Lifecycle & Coroutines
    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines)
}
