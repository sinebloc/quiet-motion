import org.gradle.plugins.signing.SigningExtension
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
    alias(libs.plugins.mavenPublish)
}

kotlin {
    // A published KMP artifact's target list is FROZEN per version, and a Maven Central
    // release can never be deleted or overwritten. So the targets below have to be right
    // before the first upload, not after: a consumer on a target that is missing here
    // cannot resolve the library at all, and the only remedy is a new version.
    //
    // iosX64 (the Intel-Mac simulator) is deliberately absent: Compose Multiplatform
    // 1.11.1 publishes no iosX64 artifacts, so the dependency cannot resolve there and
    // no consumer can be using Compose on an Intel Mac anyway. Revisit only if upstream
    // brings the target back.
    iosArm64()
    iosSimulatorArm64()

    // Compose for Desktop, and — more usefully here — a target that lets a consumer run
    // this library's behaviour in a plain JVM test without a device or a simulator.
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

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

// `-PuseGpgCmd=true` signs by shelling out to the installed gpg binary instead of
// Gradle's bundled BouncyCastle. Needed on a machine whose key GnuPG 2.4+ protected with
// AEAD/Argon2: BouncyCastle cannot decrypt those and reports it as "checksum mismatch in
// checksum of 20 bytes", which reads like a wrong passphrase and is not one. gpg itself
// has no trouble with its own keyring.
//
// Configured outside the mavenPublishing block on purpose: `signing` inside that block
// resolves to the extension's own private property, not the signing plugin's DSL.
val useGpgCmd = providers.gradleProperty("useGpgCmd").orNull.toBoolean()
if (useGpgCmd) {
    // withPlugin rather than a direct configure: the signing plugin is applied lazily by
    // signAllPublications() below, so configuring it eagerly here fails with "Extension
    // with name 'signing' does not exist". This fires whenever it is applied, in any order.
    pluginManager.withPlugin("signing") {
        extensions.configure<SigningExtension>("signing") { useGpgCmd() }
    }
}

mavenPublishing {
    // group:artifactId:version. `com.sinebloc` is the namespace verified with Central by
    // a DNS TXT record on sinebloc.com; the artifact is named for the repo rather than
    // the package so the coordinate reads as a library and not as a class path.
    coordinates("com.sinebloc", "quiet-motion", "0.1.0")

    // No host argument in 0.37.0 — the plugin targets the Central Portal by default.
    publishToMavenCentral()

    // Central rejects unsigned artifacts, but signing every publication unconditionally
    // also breaks `publishToMavenLocal` on a machine with no GPG key — which is the one
    // command worth running before a real release. So: sign when a key is configured.
    //
    // Put the credentials in ~/.gradle/gradle.properties, NEVER in this repo:
    //   mavenCentralUsername / mavenCentralPassword   (Central Portal user token)
    //   signing.keyId / signing.password / signing.secretKeyRingFile
    // or, for CI, the environment-variable form:
    //   ORG_GRADLE_PROJECT_mavenCentralUsername / ...Password
    //   ORG_GRADLE_PROJECT_signingInMemoryKey  (ascii-armored private key)
    //   ORG_GRADLE_PROJECT_signingInMemoryKeyId / ...KeyPassword
    // Checks the environment form too, not just Gradle properties: the key is often
    // supplied as ORG_GRADLE_PROJECT_signingInMemoryKey for one command rather than
    // written into a properties file, and missing that would skip signing silently.
    val hasSigningKey = listOf("signing.keyId", "signingInMemoryKey").any {
        providers.gradleProperty(it).isPresent
    } || listOf(
        "ORG_GRADLE_PROJECT_signingInMemoryKey",
        "ORG_GRADLE_PROJECT_signing_keyId",
    ).any { providers.environmentVariable(it).isPresent }
    // `-PuseGpgCmd=true` signs by shelling out to the installed gpg binary instead of
    // Gradle's bundled BouncyCastle. Needed on a machine whose key GnuPG 2.4+ protected
    // with AEAD/Argon2: BouncyCastle cannot decrypt those and reports it as
    // "checksum mismatch in checksum of 20 bytes", which reads like a wrong passphrase
    // and is not one. gpg itself has no such trouble with its own keyring.
    if (hasSigningKey || useGpgCmd) {
        signAllPublications()
    } else {
        logger.lifecycle(
            "quiet-motion: no GPG key configured, publications will NOT be signed. " +
                "Fine for publishToMavenLocal; Maven Central will reject the upload.",
        )
    }

    pom {
        name.set("quiet-motion")
        description.set(
            "Reduce-motion-safe animation primitives for Compose Multiplatform: " +
                "staggered list entrances that survive LazyColumn recycling, and one " +
                "motion scale wired to the OS accessibility setting on Android and iOS.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/sinebloc/quiet-motion")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("prosperkalu99")
                name.set("Prosper Kalu")
                url.set("https://github.com/prosperkalu99")
            }
        }
        scm {
            url.set("https://github.com/sinebloc/quiet-motion")
            connection.set("scm:git:git://github.com/sinebloc/quiet-motion.git")
            developerConnection.set("scm:git:ssh://git@github.com/sinebloc/quiet-motion.git")
        }
    }
}
