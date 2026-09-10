import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// quiet-motion: reduce-motion-safe animation primitives for Compose Multiplatform.
//
// Deliberately tiny. It depends on Compose runtime/animation/ui and on each platform's
// own accessibility API, and on nothing else.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

group = "com.sinebloc.quietmotion"
version = "0.1.0"

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.sinebloc.quietmotion"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }

        // Runs commonTest on the JVM, so the reduce-motion contract is part of the
        // normal build rather than something only a simulator can check.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // api, not implementation: every public signature here returns or accepts a
            // Compose type, so a consumer cannot use this module without them.
            api(libs.compose.runtime)
            api(libs.compose.animation)
            api(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
