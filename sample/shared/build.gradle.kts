import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The sample UI, shared by the Android app in ../androidApp and the Xcode project in
// ../iosApp. It consumes quiet-motion the way a real app does — LocalMotionScale
// provided once at the root, and no reduce-motion check anywhere else.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // A static framework, so the Xcode project has exactly one thing to link.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "com.sinebloc.quietmotion.sample.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    sourceSets {
        commonMain.dependencies {
            // The library under demonstration. A consuming project writes
            // implementation("com.sinebloc:quiet-motion:0.1.0") here instead.
            implementation(project(":"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}
