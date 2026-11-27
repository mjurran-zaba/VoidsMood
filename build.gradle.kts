// meticulous and honest
plugins {
    alias(libs.plugins.android.application) apply false
    // DINONAKTIFKAN: AGP 9.0+ sudah memiliki dukungan bawaan untuk Kotlin, plugin 'kotlin.android' tidak tersedia pada Kotlin 2.4.21
    // alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    base
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}
