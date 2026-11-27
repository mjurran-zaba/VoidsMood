// meticulous and honest
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // DINONAKTIFKAN: AGP 9.0+ sudah memiliki dukungan bawaan untuk Kotlin (agp-built-in-kotlin),
    // menerapkan plugin 'org.jetbrains.kotlin.android' menyebabkan PluginApplicationException pada Gradle 9.x.
    // alias(libs.plugins.kotlin.android)
    // BARU: Hanya terapkan plugin aplikasi Android dan compiler Jetpack Compose
    alias(libs.plugins.compose.compiler)
}

val releaseKeystoreFile = file("keystore/release.keystore")
val hasReleaseSigning = releaseKeystoreFile.exists() || System.getenv("STORE_PASSWORD") != null

android {
    namespace = "moe.rukamori.voidsmood"
    // DINONAKTIFKAN: compileSdk 35 menyebabkan error dependensi CheckAarMetadata karena activity-compose 1.13.0 butuh SDK 36/37
    // compileSdk = 35
    // BARU: Gunakan compileSdk 37 dan compileSdkMinor 2 sesuai standar VoidTune
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "moe.rukamori.voidsmood"
        minSdk = 26
        // DINONAKTIFKAN: targetSdk 35
        // targetSdk = 35
        // DINONAKTIFKAN: targetSdk 37 menyebabkan error 'Aplikasi tidak kompatibel' pada OS Android 14 Go Infinix Smart 9 HD
        // targetSdk = 37
        // BARU: targetSdk 34 (Android 14) stabil & kompatibel 100% dengan Infinix Smart 9 HD
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    splits {
        abi {
            isEnable = true
            // DINONAKTIFKAN: 'reset' adalah pemanggilan method reset(), bukan properti 'isReset'
            // isReset = true
            // BARU: Panggil fungsi reset() untuk membersihkan daftar ABI default sebelum include
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            // BARU: Aktifkan V1 (JAR) dan V2 APK Signature Scheme untuk kompatibilitas penuh Android Go
            enableV1Signing = true
            enableV2Signing = true
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

// BARU: Konfigurasi target JVM 21 untuk task kompilasi Kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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
    // DINONAKTIFKAN: libs.coroutines adalah sub-accessor objek, bukan dependensi langsung
    // implementation(libs.coroutines)
    // BARU: Gunakan libs.coroutines.core untuk dependensi kotlinx-coroutines
    implementation(libs.coroutines.core)

    // BARU: Harta Karun NewPipeExtractor, OkHttp3, dan Mesin JS Rhino
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    implementation(libs.rhino)

    // BARU: Coil 3 untuk Pemuatan Gambar Asinkron yang Hemat RAM
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // BARU: Media3 ExoPlayer Core untuk Pemutaran Video & Audio
    implementation(libs.media3)
    implementation(libs.media3.okhttp)
}
